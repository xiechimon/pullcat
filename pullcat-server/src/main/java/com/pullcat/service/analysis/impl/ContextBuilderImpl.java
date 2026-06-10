package com.pullcat.service.analysis.impl;

import com.pullcat.dto.resp.FileContentRespDTO;
import com.pullcat.dto.resp.PRMetadataRespDTO;
import com.pullcat.remote.GitHubApiService;
import com.pullcat.service.analysis.ContextBuilder;
import com.pullcat.service.analysis.TokenBudgetManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
@RequiredArgsConstructor
public class ContextBuilderImpl implements ContextBuilder {

    private static final Pattern JAVA_IMPORT = Pattern.compile("^import\\s+([\\w.]+\\*?)\\s*;");
    private static final Pattern TS_IMPORT = Pattern.compile("import\\s+.*?from\\s+['\\\"]([^'\\\"]+)['\\\"]");
    private static final Pattern TS_REQUIRE = Pattern.compile("require\\(['\\\"]([^'\\\"]+)['\\\"]\\)");
    private static final Pattern PYTHON_IMPORT = Pattern.compile("^(?:from|import)\\s+([\\w.]+)");

    private final GitHubApiService gitHubApiService;
    private final TokenBudgetManager tokenBudgetManager;

    @Override
    public String buildPRInfo(PRMetadataRespDTO meta) {
        return String.format("""
                        ## PR 信息
                        标题: %s
                        描述: %s
                        分支: %s \u2192 %s
                        变更文件: %d (+%d -%d)
                        """, meta.getTitle(), meta.getDescription(),
                meta.getHeadBranch(), meta.getBaseBranch(),
                meta.getFileCount(), meta.getAdditions(), meta.getDeletions());
    }

    @Override
    public String buildDiscussionSection(String discussion) {
        if (discussion == null || discussion.isEmpty()) {
            return "";
        }
        return discussion;
    }

    @Override
    public String buildFileTreeSection(String fileTree) {
        if (fileTree == null || fileTree.isEmpty()) {
            return "## 项目结构\n目录树不可用\n";
        }
        return "## 项目结构\n```\n" + fileTree + "\n```\n";
    }

    @Override
    public String buildChangedFilesSection(List<FileContentRespDTO> files) {
        StringBuilder sb = new StringBuilder();
        sb.append("## 变更文件\n\n");

        for (FileContentRespDTO file : files) {
            if (file.isExcluded()) {
                continue;
            }
            sb.append("### 文件: ").append(file.getPath()).append("\n\n");

            if (file.getDiff() != null && !file.getDiff().isEmpty()) {
                sb.append("#### Diff\n```diff\n");
                sb.append(truncateIfNeeded(file.getDiff(), 8000));
                sb.append("\n```\n\n");
            }

            if (file.getContent() != null && !file.getContent().isEmpty()) {
                sb.append("#### 完整文件内容\n```");
                sb.append(detectLanguage(file.getPath()));
                sb.append("\n");
                sb.append(truncateIfNeeded(file.getContent(), 12000));
                sb.append("\n```\n\n");
            }
        }
        return sb.toString();
    }

    @Override
    public Map<String, String> buildVariables(PRMetadataRespDTO meta, String fileTree, List<FileContentRespDTO> files) {
        return buildVariables(meta, fileTree, files, "", "");
    }

    @Override
    public Map<String, String> buildVariables(PRMetadataRespDTO meta, String fileTree, List<FileContentRespDTO> files,
                                              String discussion, String relatedFiles) {
        Map<String, String> vars = new LinkedHashMap<>();
        vars.put("pr_info", buildPRInfo(meta));
        vars.put("pr_discussion", discussion != null && !discussion.isEmpty() ? discussion : "");
        vars.put("file_tree", buildFileTreeSection(fileTree));
        vars.put("changed_files", buildChangedFilesSection(files));
        vars.put("related_files", relatedFiles != null && !relatedFiles.isEmpty() ? relatedFiles : "");
        return vars;
    }

    @Override
    public List<String> extractImports(FileContentRespDTO file) {
        String lang = detectLanguage(file.getPath());
        String content = file.getContent();
        if (content == null) {
            return Collections.emptyList();
        }

        List<String> imports = new ArrayList<>();
        for (String line : content.split("\n")) {
            String trimmed = line.trim();
            if (lang.equals("java")) {
                Matcher matcher = JAVA_IMPORT.matcher(trimmed);
                if (matcher.find()) {
                    imports.add(matcher.group(1));
                }
            } else if (lang.equals("typescript") || lang.equals("javascript")) {
                Matcher importMatcher = TS_IMPORT.matcher(trimmed);
                if (importMatcher.find()) {
                    imports.add(importMatcher.group(1));
                } else {
                    Matcher requireMatcher = TS_REQUIRE.matcher(trimmed);
                    if (requireMatcher.find()) {
                        imports.add(requireMatcher.group(1));
                    }
                }
            } else if (lang.equals("python")) {
                Matcher matcher = PYTHON_IMPORT.matcher(trimmed);
                if (matcher.find()) {
                    imports.add(matcher.group(1));
                }
            }
        }
        return imports.stream().distinct().toList();
    }

    @Override
    public List<String> resolveLocalImports(List<String> imports, String fileTree) {
        Set<String> resolved = new LinkedHashSet<>();
        for (String imp : imports) {
            if (isExternalLibrary(imp)) {
                continue;
            }
            String path = importToFilePath(imp);
            if (path != null && fileTree.contains(path)) {
                resolved.add(path);
            }
        }
        return new ArrayList<>(resolved);
    }

    @Override
    public String buildRelatedFilesSection(GitHubApiService.PRUrl prUrl, List<String> resolvedPaths) {
        if (resolvedPaths.isEmpty()) {
            return "";
        }

        long budget = (long) (tokenBudgetManager.getMaxTokens() * 0.3);
        StringBuilder sb = new StringBuilder();
        sb.append("## 相关依赖文件\n\n");

        for (String path : resolvedPaths) {
            if (tokenBudgetManager.estimateTokens(sb.toString()) > budget) {
                break;
            }

            try {
                String content = gitHubApiService.fetchFileContent(prUrl, path).block();
                if (content == null || content.isEmpty()) {
                    continue;
                }

                sb.append("### 依赖: ").append(path).append("\n```");
                sb.append(detectLanguage(path)).append("\n");
                sb.append(truncateIfNeeded(content, 3000));
                sb.append("\n```\n\n");
            } catch (Exception ignored) {
                // skip unavailable files
            }
        }
        return sb.toString();
    }

    static String detectLanguage(String path) {
        String lower = path.toLowerCase();
        if (lower.endsWith(".java")) return "java";
        if (lower.endsWith(".kt")) return "kotlin";
        if (lower.endsWith(".py")) return "python";
        if (lower.endsWith(".ts") || lower.endsWith(".tsx")) return "typescript";
        if (lower.endsWith(".js") || lower.endsWith(".jsx")) return "javascript";
        if (lower.endsWith(".go")) return "go";
        if (lower.endsWith(".rs")) return "rust";
        if (lower.endsWith(".sql")) return "sql";
        if (lower.endsWith(".xml")) return "xml";
        if (lower.endsWith(".json")) return "json";
        if (lower.endsWith(".yml") || lower.endsWith(".yaml")) return "yaml";
        if (lower.endsWith(".sh")) return "bash";
        if (lower.endsWith(".md")) return "markdown";
        if (lower.endsWith(".css")) return "css";
        if (lower.endsWith(".html")) return "html";
        return "";
    }

    private boolean isExternalLibrary(String imp) {
        return imp.startsWith("java.") || imp.startsWith("javax.")
                || imp.startsWith("jakarta.") || imp.startsWith("org.springframework.")
                || imp.startsWith("react") || imp.equals("react")
                || imp.startsWith("lodash") || imp.startsWith("@")
                || imp.startsWith("os") || imp.startsWith("sys")
                || imp.startsWith("http") || !imp.contains(".");
    }

    private String importToFilePath(String imp) {
        if (imp.startsWith("./") || imp.startsWith("../")) {
            return imp.replaceFirst("^\\./", "").replaceFirst("^\\.\\./", "");
        }
        return imp.replace('.', '/') + ".java";
    }

    private String truncateIfNeeded(String text, int maxLength) {
        if (text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, maxLength) + "\n... [截断，" + (text.length() - maxLength) + " 字符]";
    }
}
