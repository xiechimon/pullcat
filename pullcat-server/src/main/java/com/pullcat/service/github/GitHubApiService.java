package com.pullcat.service.github;

import com.fasterxml.jackson.databind.JsonNode;
import com.pullcat.config.GitHubConfig;
import com.pullcat.model.FileContent;
import com.pullcat.model.GitHubFile;
import com.pullcat.model.PRData;
import com.pullcat.model.PRMetadata;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * GitHub API 服务，封装与 GitHub REST API 的交互逻辑。
 * <p>
 * 提供 PR URL 解析、PR 元数据拉取、diff 获取、变更文件内容和目录树获取等功能。
 * 所有 API 调用均以响应式（Mono/Flux）方式返回。
 */
@Slf4j
@Service
public class GitHubApiService {

    private static final Pattern PR_URL_PATTERN =
            Pattern.compile("https?://github\\.com/([^/]+)/([^/]+)/pull/(\\d+).*");

    private static final Set<String> BINARY_EXTENSIONS = Set.of(
            "png", "jpg", "jpeg", "gif", "bmp", "ico", "svg",
            "pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx",
            "zip", "tar", "gz", "rar", "7z",
            "exe", "dll", "so", "dylib",
            "mp3", "mp4", "avi", "mov", "wav",
            "ttf", "otf", "woff", "woff2", "eot",
            "jar", "war", "ear", "class",
            "db", "sqlite", "sqlite3",
            "lock", "sum"
    );

    private static final String[] GENERATED_PATH_PATTERNS = {
            "generated/", "generated-src/", "target/", "build/", "dist/",
            "node_modules/", ".git/", "__pycache__/", "vendor/"
    };

    private final WebClient webClient;

    /**
     * 构造函数，通过 {@link GitHubConfig} 获取 PAT Token 并初始化 WebClient。
     */
    @Autowired
    public GitHubApiService(GitHubConfig config) {
        this(WebClient.builder()
                .baseUrl("https://api.github.com")
                .defaultHeader("Authorization", "Bearer " + config.getToken())
                .defaultHeader("Accept", "application/vnd.github.v3+json")
                .defaultHeader("User-Agent", "pullcat")
                .build());
    }

    /**
     * 内部构造器，用于注入自定义 WebClient（单元测试等场景）。
     */
    GitHubApiService(WebClient webClient) {
        this.webClient = webClient;
    }

    /**
     * 解析 GitHub PR URL，提取 owner、repo 和 PR 编号。
     *
     * @param url 待解析的 GitHub PR URL，格式为 {@code https://github.com/owner/repo/pull/number}
     * @return 包含 owner、repo 和 PR 编号的 {@link PRUrl} 记录
     * @throws IllegalArgumentException 如果 URL 格式不匹配
     */
    public PRUrl parsePrUrl(String url) {
        Matcher matcher = PR_URL_PATTERN.matcher(url.trim());
        if (!matcher.matches()) {
            throw new IllegalArgumentException(
                    "Invalid GitHub PR URL. Expected format: https://github.com/owner/repo/pull/number");
        }
        return new PRUrl(matcher.group(1), matcher.group(2), Integer.parseInt(matcher.group(3)));
    }

    /**
     * 获取 PR 的元数据信息（标题、描述、分支、变更统计等）。
     *
     * @param prUrl 解析后的 PR URL 信息（owner、repo、PR 编号）
     * @return 包含 PR 元数据的 {@code Mono<PRMetadata>} 响应式包装
     */
    public Mono<PRMetadata> fetchPRMetadata(PRUrl prUrl) {
        return webClient.get()
                .uri("/repos/{owner}/{repo}/pulls/{number}", prUrl.owner(), prUrl.repo(), prUrl.number())
                .retrieve()
                .bodyToMono(JsonNode.class)
                .map(json -> mapToPRMetadata(prUrl, json))
                .doOnSuccess(meta -> log.debug("GitHub API call: fetchPRMetadata"));
    }

    PRMetadata mapToPRMetadata(PRUrl prUrl, JsonNode json) {
        PRMetadata meta = new PRMetadata();
        meta.setOwner(prUrl.owner());
        meta.setRepo(prUrl.repo());
        meta.setPullNumber(prUrl.number());
        meta.setTitle(json.path("title").asText(""));
        meta.setDescription(json.path("body").asText(""));
        meta.setBaseBranch(json.path("base").path("ref").asText(""));
        meta.setHeadBranch(json.path("head").path("ref").asText(""));
        meta.setFileCount(json.path("changed_files").asInt(0));
        meta.setAdditions(json.path("additions").asInt(0));
        meta.setDeletions(json.path("deletions").asInt(0));
        return meta;
    }

    /**
     * 获取 PR 的 issue comments 和 review comments，合并后返回格式化的讨论文本。
     */
    public Mono<String> fetchPRComments(PRUrl prUrl) {
        var issueComments = webClient.get()
                .uri("/repos/{owner}/{repo}/issues/{number}/comments",
                        prUrl.owner(), prUrl.repo(), prUrl.number())
                .retrieve()
                .bodyToFlux(JsonNode.class)
                .map(json -> formatComment(json, "issue"))
                .collectList();

        var reviewComments = webClient.get()
                .uri("/repos/{owner}/{repo}/pulls/{number}/comments",
                        prUrl.owner(), prUrl.repo(), prUrl.number())
                .retrieve()
                .bodyToFlux(JsonNode.class)
                .map(json -> formatComment(json, "review"))
                .collectList();

        return Mono.zip(issueComments, reviewComments)
                .map(tuple -> {
                    List<String> all = new ArrayList<>();
                    all.addAll(tuple.getT1());
                    all.addAll(tuple.getT2());
                    if (all.isEmpty()) return "";
                    return "## PR 讨论\n" + String.join("\n", all) + "\n";
                });
    }

    private String formatComment(JsonNode json, String type) {
        String user = json.path("user").path("login").asText("unknown");
        String body = json.path("body").asText("");
        if (body.length() > 500) body = body.substring(0, 500) + "...";
        return String.format("@%s: %s", user, body);
    }

    /**
     * 获取单个文件的原始内容（用于依赖文件获取）。
     */
    public Mono<String> fetchFileContent(PRUrl prUrl, String path) {
        return webClient.get()
                .uri("/repos/{owner}/{repo}/contents/{path}?ref={ref}",
                        prUrl.owner(), prUrl.repo(), path, prUrl.ref())
                .header("Accept", "application/vnd.github.v3.raw")
                .retrieve()
                .bodyToMono(String.class)
                .onErrorReturn("");
    }

    /**
     * 更新 PR 的 Commit Status（用于显示审查进度）。
     */
    public Mono<Void> updateCommitStatus(PRUrl prUrl, String sha, String state, String description) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("state", state);
        body.put("description", description);
        body.put("context", "pullcat/code-review");

        return webClient.post()
                .uri("/repos/{owner}/{repo}/statuses/{sha}", prUrl.owner(), prUrl.repo(), sha)
                .bodyValue(body)
                .retrieve()
                .bodyToMono(Void.class);
    }

    /**
     * 获取 PR 的 head commit SHA。
     */
    public Mono<String> fetchHeadSha(PRUrl prUrl) {
        return webClient.get()
                .uri("/repos/{owner}/{repo}/pulls/{number}", prUrl.owner(), prUrl.repo(), prUrl.number())
                .retrieve()
                .bodyToMono(JsonNode.class)
                .map(json -> json.path("head").path("sha").asText(""));
    }

    /**
     * 建议 PR reviewer（简单实现：返回仓库 collaborators）。
     */
    public Mono<PRData> fetchPRData(PRUrl prUrl) {
        return Mono.zip(
                fetchPRMetadata(prUrl),
                fetchDiff(prUrl),
                fetchChangedFiles(prUrl),
                fetchFileTree(prUrl)
        ).flatMap(tuple -> {
            PRMetadata metadata = tuple.getT1();
            String diff = tuple.getT2();
            List<GitHubFile> changedFiles = tuple.getT3();
            String fileTree = tuple.getT4();
            return fetchFileContents(prUrl, changedFiles)
                    .collectList()
                    .map(fileContents -> {
                        PRData prData = new PRData();
                        prData.setMetadata(metadata);
                        prData.setDiff(diff);
                        prData.setFiles(fileContents);
                        prData.setFileTree(fileTree);
                        return prData;
                    });
        });
    }

    /**
     * 获取 PR 的 diff 内容（unified diff 格式）。
     *
     * @param prUrl 解析后的 PR URL 信息
     * @return 包含 unified diff 文本的 {@code Mono<String>}
     */
    public Mono<String> fetchDiff(PRUrl prUrl) {
        return webClient.get()
                .uri("/repos/{owner}/{repo}/pulls/{number}", prUrl.owner(), prUrl.repo(), prUrl.number())
                .header("Accept", "application/vnd.github.v3.diff")
                .retrieve()
                .bodyToMono(String.class);
    }

    /**
     * 获取 PR 中变更的文件列表。
     *
     * @param prUrl 解析后的 PR URL 信息
     * @return 包含变更文件列表的 {@code Mono<List<GitHubFile>>}
     */
    public Mono<List<GitHubFile>> fetchChangedFiles(PRUrl prUrl) {
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/repos/{owner}/{repo}/pulls/{number}/files")
                        .queryParam("per_page", 100)
                        .build(prUrl.owner(), prUrl.repo(), prUrl.number()))
                .retrieve()
                .bodyToFlux(GitHubFile.class)
                .collectList();
    }

    /**
     * 批量获取变更文件的完整内容，自动过滤二进制文件和生成目录。
     * 单个文件获取失败时返回占位文本，不中断整体流程。
     *
     * @param prUrl        解析后的 PR URL 信息
     * @param changedFiles PR 中变更的文件列表
     * @return 包含文件内容的 {@code Flux<FileContent>} 响应式流
     */
    public Flux<FileContent> fetchFileContents(PRUrl prUrl, List<GitHubFile> changedFiles) {
        return Flux.fromIterable(changedFiles)
                .filter(file -> !shouldExcludeFile(file.getFilename()))
                .flatMap(file -> fetchSingleFileContent(prUrl, file)
                        .map(content -> new FileContent(file.getFilename(), content, ""))
                        .onErrorResume(e -> {
                            log.warn("Failed to fetch content for {}: {}", file.getFilename(), e.getMessage());
                            return Mono.just(new FileContent(file.getFilename(), "[Content unavailable]", ""));
                        }));
    }

    private Mono<String> fetchSingleFileContent(PRUrl prUrl, GitHubFile file) {
        return webClient.get()
                .uri("/repos/{owner}/{repo}/contents/{path}?ref={ref}",
                        prUrl.owner(), prUrl.repo(), file.getFilename(), prUrl.ref())
                .header("Accept", "application/vnd.github.v3.raw")
                .retrieve()
                .bodyToMono(String.class)
                .onErrorResume(e -> Mono.just("[Binary or large file - content not fetched]"));
    }

    /**
     * 获取仓库的完整目录树结构（递归模式），按目录分组排列。
     *
     * @param prUrl 解析后的 PR URL 信息
     * @return 包含格式化目录树字符串的 {@code Mono<String>}
     */
    public Mono<String> fetchFileTree(PRUrl prUrl) {
        return webClient.get()
                .uri("/repos/{owner}/{repo}/git/trees/{ref}?recursive=1",
                        prUrl.owner(), prUrl.repo(), prUrl.headRef())
                .retrieve()
                .bodyToMono(JsonNode.class)
                .map(json -> {
                    JsonNode tree = json.path("tree");
                    if (tree.isMissingNode() || !tree.isArray()) {
                        return "File tree unavailable";
                    }

                    Map<String, List<String>> dirMap = new TreeMap<>();
                    for (JsonNode node : tree) {
                        String path = node.path("path").asText("");
                        String type = node.path("type").asText("");
                        if (!path.isEmpty() && "blob".equals(type)) {
                            String dir = path.contains("/") ? path.substring(0, path.lastIndexOf('/')) : ".";
                            dirMap.computeIfAbsent(dir, k -> new ArrayList<>()).add(path);
                        }
                    }

                    StringBuilder sb = new StringBuilder();
                    for (Map.Entry<String, List<String>> entry : dirMap.entrySet()) {
                        sb.append(entry.getKey()).append("/\n");
                        for (String file : entry.getValue()) {
                            String name = file.substring(file.lastIndexOf('/') + 1);
                            sb.append("  ").append(name).append("\n");
                        }
                        sb.append("\n");
                    }
                    return sb.toString();
                })
                .onErrorReturn("File tree unavailable");
    }

    /**
     * 判断文件是否应被排除（二进制、生成目录或特殊文件）。
     */
    boolean shouldExcludeFile(String filename) {
        String lower = filename.toLowerCase();
        int dotIndex = lower.lastIndexOf('.');
        if (dotIndex > 0) {
            String ext = lower.substring(dotIndex + 1);
            if (BINARY_EXTENSIONS.contains(ext)) {
                return true;
            }
        }
        for (String pattern : GENERATED_PATH_PATTERNS) {
            if (lower.contains(pattern)) {
                return true;
            }
        }
        String name = lower.substring(lower.lastIndexOf('/') + 1);
        return name.startsWith(".") || "package-lock.json".equals(name) || "yarn.lock".equals(name);
    }

    // ─── 阶段 4：发布 PR Review ─────────────────────────────────

    /**
     * 发布 PR 审查评论到 GitHub，支持摘要正文和可选的行级 inline 评论。
     *
     * @param prUrl       解析后的 PR URL 信息
     * @param summaryBody 审查摘要正文（Markdown 格式）
     * @param comments    行级评论列表，file 或 line 为 null 的评论将被过滤
     * @return 包含 GitHub Review ID 的 {@code Mono<Long>}
     */
    public Mono<Long> publishReviewWithComments(PRUrl prUrl, String summaryBody, List<ReviewComment> comments) {
        Map<String, Object> body = buildReviewBody(summaryBody, comments);

        return webClient.post()
                .uri("/repos/{owner}/{repo}/pulls/{number}/reviews",
                        prUrl.owner(), prUrl.repo(), prUrl.number())
                .bodyValue(body)
                .retrieve()
                .bodyToMono(JsonNode.class)
                .map(json -> json.path("id").asLong());
    }

    /**
     * 构建 PR Review 请求体，包含 event、body 和可选的 inline comments 数组。
     */
    Map<String, Object> buildReviewBody(String summaryBody, List<ReviewComment> comments) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("event", "COMMENT");
        body.put("body", summaryBody);

        if (!comments.isEmpty()) {
            List<Map<String, Object>> commentList = comments.stream()
                    .filter(c -> c.file() != null && c.line() != null)
                    .map(c -> {
                        Map<String, Object> cm = new LinkedHashMap<>();
                        cm.put("path", c.file());
                        cm.put("line", c.line());
                        cm.put("side", "RIGHT");
                        cm.put("body", c.body());
                        return cm;
                    })
                    .toList();
            body.put("comments", commentList);
        }
        return body;
    }

    /**
     * 仅发布摘要正文（无行级评论）的便捷方法。
     */
    public Mono<Long> publishReview(PRUrl prUrl, String summaryBody) {
        return publishReviewWithComments(prUrl, summaryBody, List.of());
    }

    /**
     * 行级审查评论，指定文件路径、行号和评论内容。
     */
    public record ReviewComment(String file, Integer line, String body) {}

    /**
     * 解析后的 GitHub PR URL 信息。
     */
    public record PRUrl(String owner, String repo, int number) {
        public String ref() {
            return "heads/" + number;
        }

        public String headRef() {
            return number > 0 ? String.valueOf(number) : "main";
        }
    }
}
