package com.pullcat.service.analysis;

import com.pullcat.model.FileContent;
import com.pullcat.model.PRMetadata;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * 上下文构建器，将 PR 原始数据组装为 LLM 可理解的 prompt 上下文字符串。
 */
@Component
public class ContextBuilder {

    /**
     * 构建 PR 信息摘要文本。
     */
    public String buildPRInfo(PRMetadata meta) {
        return String.format("""
                        ##  PR 信息
                        标题: %s
                        描述: %s
                        分支: %s → %s
                        变更文件: %d (+%d -%d)
                        """, meta.getTitle(), meta.getDescription(),
                meta.getHeadBranch(), meta.getBaseBranch(),
                meta.getFileCount(), meta.getAdditions(), meta.getDeletions());
    }

    /**
     * 构建项目目录树文本段。
     */
    public String buildFileTreeSection(String fileTree) {
        if (fileTree == null || fileTree.isEmpty()) {
            return "## 项目结构\n目录树不可用\n";
        }
        return "## 项目结构\n```\n" + fileTree + "\n```\n";
    }

    /**
     * 构建变更文件详情文本段，每个文件包含 diff 和完整内容。
     */
    public String buildChangedFilesSection(List<FileContent> files) {
        StringBuilder sb = new StringBuilder();
        sb.append("## 变更文件\n\n");

        for (FileContent file : files) {
            if (file.isExcluded()) continue;
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

    /**
     * 构建完整的 prompt 上下文变量 Map。
     */
    public Map<String, String> buildVariables(PRMetadata meta, String fileTree, List<FileContent> files) {
        return Map.of(
                "pr_info", buildPRInfo(meta),
                "file_tree", buildFileTreeSection(fileTree),
                "changed_files", buildChangedFilesSection(files)
        );
    }

    private String detectLanguage(String path) {
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
        return "";
    }

    private String truncateIfNeeded(String text, int maxLength) {
        if (text.length() <= maxLength) return text;
        return text.substring(0, maxLength) + "\n... [截断，" + (text.length() - maxLength) + " 字符]";
    }
}
