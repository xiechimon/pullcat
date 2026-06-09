package com.pullcat.remote;

import com.pullcat.common.convention.exception.GitHubForbiddenException;
import com.pullcat.config.infra.GitHubConfig;
import com.pullcat.dto.resp.FileContentRespDTO;
import com.pullcat.remote.dto.req.GitHubCommitStatusReqDTO;
import com.pullcat.remote.dto.req.GitHubReviewCommentReqDTO;
import com.pullcat.remote.dto.req.GitHubReviewReqDTO;
import com.pullcat.remote.dto.resp.GitHubCommentRespDTO;
import com.pullcat.remote.dto.resp.GitHubFileRespDTO;
import com.pullcat.remote.dto.resp.GitHubPullRequestRespDTO;
import com.pullcat.remote.dto.resp.GitHubReviewRespDTO;
import com.pullcat.remote.dto.resp.GitHubTreeNodeRespDTO;
import com.pullcat.remote.dto.resp.GitHubTreeRespDTO;
import com.pullcat.dto.resp.PRDataRespDTO;
import com.pullcat.dto.resp.PRMetadataRespDTO;
import com.pullcat.toolkit.RetryPolicy;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;
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
    private final MeterRegistry meterRegistry;
    private final GitHubConfig config;
    private final OAuth2AuthorizedClientService oauth2ClientService;

    @Autowired
    public GitHubApiService(GitHubConfig config, MeterRegistry meterRegistry,
                            OAuth2AuthorizedClientService oauth2ClientService) {
        this.config = config;
        this.oauth2ClientService = oauth2ClientService;
        this.webClient = WebClient.builder()
                .baseUrl("https://api.github.com")
                .defaultHeader("Accept", "application/vnd.github.v3+json")
                .defaultHeader("User-Agent", "pullcat")
                .filter(authFilter())
                .filter(forbiddenHandler())
                .clientConnector(new org.springframework.http.client.reactive.ReactorClientHttpConnector(
                        HttpClient.create().responseTimeout(Duration.ofSeconds(30))))
                .build();
        this.meterRegistry = meterRegistry;
    }

    GitHubApiService(WebClient webClient) {
        this(webClient, null);
    }

    GitHubApiService(WebClient webClient, MeterRegistry meterRegistry) {
        this.webClient = webClient;
        this.meterRegistry = meterRegistry;
        this.config = null;
        this.oauth2ClientService = null;
    }

    private String resolveToken() {
        if (oauth2ClientService != null) {
            var auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth instanceof OAuth2AuthenticationToken oauthToken) {
                OAuth2AuthorizedClient client = oauth2ClientService.loadAuthorizedClient(
                        oauthToken.getAuthorizedClientRegistrationId(), oauthToken.getName());
                if (client != null && client.getAccessToken() != null) {
                    return client.getAccessToken().getTokenValue();
                }
            }
        }
        return config != null ? config.getToken() : null;
    }

    private ExchangeFilterFunction authFilter() {
        return ExchangeFilterFunction.ofRequestProcessor(request -> {
            String token = resolveToken();
            if (token != null) {
                return Mono.just(ClientRequest.from(request)
                        .headers(h -> h.setBearerAuth(token))
                        .build());
            }
            return Mono.just(request);
        });
    }

    private ExchangeFilterFunction forbiddenHandler() {
        return ExchangeFilterFunction.ofResponseProcessor(response -> {
            if (response.statusCode() == HttpStatus.FORBIDDEN) {
                return response.bodyToMono(String.class)
                        .flatMap(body -> Mono.error(new GitHubForbiddenException(body)));
            }
            return Mono.just(response);
        });
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
     * @return 包含 PR 元数据的 {@code Mono<PRMetadataRespDTO>} 响应式包装
     */
    public Mono<PRMetadataRespDTO> fetchPRMetadata(PRUrl prUrl) {
        return webClient.get()
                .uri("/repos/{owner}/{repo}/pulls/{number}", prUrl.owner(), prUrl.repo(), prUrl.number())
                .retrieve()
                .bodyToMono(GitHubPullRequestRespDTO.class)
                .retryWhen(RetryPolicy.githubRetry())
                .map(response -> mapToPRMetadata(prUrl, response))
                .doOnSuccess(meta -> {
                    recordApiCall("pulls", "success");
                    log.debug("GitHub API call: fetchPRMetadata");
                })
                .doOnError(e -> recordApiCall("pulls", "error"));
    }

    PRMetadataRespDTO mapToPRMetadata(PRUrl prUrl, GitHubPullRequestRespDTO response) {
        PRMetadataRespDTO meta = new PRMetadataRespDTO();
        meta.setOwner(prUrl.owner());
        meta.setRepo(prUrl.repo());
        meta.setPullNumber(prUrl.number());
        meta.setTitle(response.getTitle() != null ? response.getTitle() : "");
        meta.setDescription(response.getBody() != null ? response.getBody() : "");
        meta.setBaseBranch(response.getBase() != null && response.getBase().getRef() != null ? response.getBase().getRef() : "");
        meta.setHeadBranch(response.getHead() != null && response.getHead().getRef() != null ? response.getHead().getRef() : "");
        meta.setFileCount(response.getChangedFiles());
        meta.setAdditions(response.getAdditions());
        meta.setDeletions(response.getDeletions());
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
                .bodyToFlux(GitHubCommentRespDTO.class)
                .map(this::formatComment)
                .collectList();

        var reviewComments = webClient.get()
                .uri("/repos/{owner}/{repo}/pulls/{number}/comments",
                        prUrl.owner(), prUrl.repo(), prUrl.number())
                .retrieve()
                .bodyToFlux(GitHubCommentRespDTO.class)
                .map(this::formatComment)
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

    private String formatComment(GitHubCommentRespDTO comment) {
        String user = comment.getUser() != null && comment.getUser().getLogin() != null
                ? comment.getUser().getLogin() : "unknown";
        String body = comment.getBody() != null ? comment.getBody() : "";
        if (body.length() > 500) {
            body = body.substring(0, 500) + "...";
        }
        return String.format("@%s: %s", user, body);
    }

    /**
     * 获取单个文件的原始内容（用于依赖文件获取）。
     */
    public Mono<String> fetchFileContent(PRUrl prUrl, String path) {
        String ref = prUrl.headRef() != null ? prUrl.headRef() : "main";
        return webClient.get()
                .uri("/repos/{owner}/{repo}/contents/{path}?ref={ref}",
                        prUrl.owner(), prUrl.repo(), path, ref)
                .header("Accept", "application/vnd.github.v3.raw")
                .retrieve()
                .bodyToMono(String.class)
                .retryWhen(RetryPolicy.githubRetry())
                .onErrorReturn("");
    }

    /**
     * 更新 PR 的 Commit Status（用于显示审查进度）。
     */
    public Mono<Void> updateCommitStatus(PRUrl prUrl, String sha, String state, String description) {
        GitHubCommitStatusReqDTO body = new GitHubCommitStatusReqDTO();
        body.setState(state);
        body.setDescription(description);
        body.setContext("pullcat/code-review");

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
                .bodyToMono(GitHubPullRequestRespDTO.class)
                .map(response -> response.getHead() != null && response.getHead().getSha() != null
                        ? response.getHead().getSha() : "");
    }

    /**
     * 获取 PR 完整数据：先拉取 metadata 获取 head ref，再以正确的 ref 拉取其余数据。
     */
    public Mono<PRDataRespDTO> fetchPRData(PRUrl prUrl) {
        return fetchPRMetadata(prUrl)
                .flatMap(metadata -> {
                    PRUrl enriched = prUrl.withHeadInfo(
                            metadata.getHeadBranch(), metadata.getHeadBranch());
                    return Mono.zip(
                            fetchDiff(enriched),
                            fetchChangedFiles(enriched),
                            fetchFileTree(enriched)
                    ).flatMap(tuple -> {
                        String diff = tuple.getT1();
                        List<GitHubFileRespDTO> changedFiles = tuple.getT2();
                        String fileTree = tuple.getT3();
                        return fetchFileContents(enriched, changedFiles)
                                .collectList()
                                .map(fileContents -> {
                                    PRDataRespDTO prData = new PRDataRespDTO();
                                    prData.setMetadata(metadata);
                                    prData.setDiff(diff);
                                    prData.setFiles(fileContents);
                                    prData.setFileTree(fileTree);
                                    return prData;
                                });
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
     * @return 包含变更文件列表的 {@code Mono<List<GitHubFileRespDTO>>}
     */
    public Mono<List<GitHubFileRespDTO>> fetchChangedFiles(PRUrl prUrl) {
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/repos/{owner}/{repo}/pulls/{number}/files")
                        .queryParam("per_page", 100)
                        .build(prUrl.owner(), prUrl.repo(), prUrl.number()))
                .retrieve()
                .bodyToFlux(GitHubFileRespDTO.class)
                .collectList();
    }

    /**
     * 批量获取变更文件的完整内容，自动过滤二进制文件和生成目录。
     * 单个文件获取失败时返回占位文本，不中断整体流程。
     *
     * @param prUrl        解析后的 PR URL 信息
     * @param changedFiles PR 中变更的文件列表
     * @return 包含文件内容的 {@code Flux<FileContentRespDTO>} 响应式流
     */
    public Flux<FileContentRespDTO> fetchFileContents(PRUrl prUrl, List<GitHubFileRespDTO> changedFiles) {
        return Flux.fromIterable(changedFiles)
                .filter(file -> !shouldExcludeFile(file.getFilename()))
                .flatMap(file -> fetchSingleFileContent(prUrl, file)
                        .map(content -> new FileContentRespDTO(file.getFilename(), content, ""))
                        .onErrorResume(e -> {
                            log.warn("Failed to fetch content for {}: {}", file.getFilename(), e.getMessage());
                            return Mono.just(new FileContentRespDTO(file.getFilename(), "[Content unavailable]", ""));
                        }));
    }

    private Mono<String> fetchSingleFileContent(PRUrl prUrl, GitHubFileRespDTO file) {
        String ref = prUrl.headRef() != null ? prUrl.headRef() : "main";
        return webClient.get()
                .uri("/repos/{owner}/{repo}/contents/{path}?ref={ref}",
                        prUrl.owner(), prUrl.repo(), file.getFilename(), ref)
                .header("Accept", "application/vnd.github.v3.raw")
                .retrieve()
                .bodyToMono(String.class)
                .onErrorResume(e -> {
                    log.warn("Failed to fetch single file content for {}: {}", file.getFilename(), e.getMessage());
                    return Mono.just("[Binary or large file - content not fetched]");
                });
    }

    /**
     * 获取仓库的完整目录树结构（递归模式），按目录分组排列。
     *
     * @param prUrl 解析后的 PR URL 信息
     * @return 包含格式化目录树字符串的 {@code Mono<String>}
     */
    public Mono<String> fetchFileTree(PRUrl prUrl) {
        String ref = prUrl.headRef() != null ? prUrl.headRef() : "main";
        return webClient.get()
                .uri("/repos/{owner}/{repo}/git/trees/{ref}?recursive=1",
                        prUrl.owner(), prUrl.repo(), ref)
                .retrieve()
                .bodyToMono(GitHubTreeRespDTO.class)
                .map(response -> {
                    if (response.getTree() == null || response.getTree().isEmpty()) {
                        return "File tree unavailable";
                    }

                    Map<String, List<String>> dirMap = new TreeMap<>();
                    for (GitHubTreeNodeRespDTO node : response.getTree()) {
                        String path = node.getPath() != null ? node.getPath() : "";
                        String type = node.getType() != null ? node.getType() : "";
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
        GitHubReviewReqDTO body = buildReviewBody(summaryBody, comments);

        return webClient.post()
                .uri("/repos/{owner}/{repo}/pulls/{number}/reviews",
                        prUrl.owner(), prUrl.repo(), prUrl.number())
                .bodyValue(body)
                .retrieve()
                .bodyToMono(GitHubReviewRespDTO.class)
                .retryWhen(RetryPolicy.githubRetry())
                .map(GitHubReviewRespDTO::getId);
    }

    /**
     * 构建 PR Review 请求体，包含 event、body 和可选的 inline comments 数组。
     */
    GitHubReviewReqDTO buildReviewBody(String summaryBody, List<ReviewComment> comments) {
        GitHubReviewReqDTO body = new GitHubReviewReqDTO();
        body.setEvent("COMMENT");
        body.setBody(summaryBody);

        List<GitHubReviewCommentReqDTO> commentList = comments.stream()
                .filter(c -> c.file() != null && c.line() != null)
                .map(c -> {
                    GitHubReviewCommentReqDTO comment = new GitHubReviewCommentReqDTO();
                    comment.setPath(c.file());
                    comment.setLine(c.line());
                    comment.setSide("RIGHT");
                    comment.setBody(c.body());
                    return comment;
                })
                .toList();
        if (!commentList.isEmpty()) {
            body.setComments(commentList);
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
    public record PRUrl(String owner, String repo, int number, String headRef, String headSha) {
        public PRUrl(String owner, String repo, int number) {
            this(owner, repo, number, null, null);
        }

        public PRUrl withHeadInfo(String headRef, String headSha) {
            return new PRUrl(owner, repo, number, headRef, headSha);
        }
    }

    private void recordApiCall(String endpoint, String status) {
        if (meterRegistry != null) {
            meterRegistry.counter("github_api_calls_total", "endpoint", endpoint, "status", status).increment();
        }
    }
}
