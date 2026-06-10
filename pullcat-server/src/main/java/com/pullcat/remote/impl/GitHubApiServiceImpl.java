package com.pullcat.remote.impl;

import com.pullcat.common.convention.exception.GitHubForbiddenException;
import com.pullcat.config.infra.GitHubConfig;
import com.pullcat.dto.resp.FileContentRespDTO;
import com.pullcat.dto.resp.PRDataRespDTO;
import com.pullcat.dto.resp.PRMetadataRespDTO;
import com.pullcat.remote.GitHubApiService;
import com.pullcat.remote.GitHubInstallationTokenService;
import com.pullcat.remote.dto.req.GitHubCommitStatusReqDTO;
import com.pullcat.remote.dto.req.GitHubReviewCommentReqDTO;
import com.pullcat.remote.dto.req.GitHubReviewReqDTO;
import com.pullcat.remote.dto.resp.GitHubCommentRespDTO;
import com.pullcat.remote.dto.resp.GitHubFileRespDTO;
import com.pullcat.remote.dto.resp.GitHubPullRequestRespDTO;
import com.pullcat.remote.dto.resp.GitHubReviewRespDTO;
import com.pullcat.remote.dto.resp.GitHubTreeNodeRespDTO;
import com.pullcat.remote.dto.resp.GitHubTreeRespDTO;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * GitHub API 服务实现
 */
@Slf4j
@Service
public class GitHubApiServiceImpl implements GitHubApiService {

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
    private final GitHubInstallationTokenService gitHubInstallationTokenService;

    @Autowired
    public GitHubApiServiceImpl(GitHubConfig config, MeterRegistry meterRegistry,
                                OAuth2AuthorizedClientService oauth2ClientService,
                                GitHubInstallationTokenService gitHubInstallationTokenService) {
        this.config = config;
        this.oauth2ClientService = oauth2ClientService;
        this.gitHubInstallationTokenService = gitHubInstallationTokenService;
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

    GitHubApiServiceImpl(WebClient webClient) {
        this(webClient, null);
    }

    GitHubApiServiceImpl(WebClient webClient, MeterRegistry meterRegistry) {
        this.webClient = webClient;
        this.meterRegistry = meterRegistry;
        this.config = null;
        this.oauth2ClientService = null;
        this.gitHubInstallationTokenService = null;
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

    @Override
    public PRUrl parsePrUrl(String url) {
        Matcher matcher = PR_URL_PATTERN.matcher(url.trim());
        if (!matcher.matches()) {
            throw new IllegalArgumentException(
                    "Invalid GitHub PR URL. Expected format: https://github.com/owner/repo/pull/number");
        }
        return new PRUrl(matcher.group(1), matcher.group(2), Integer.parseInt(matcher.group(3)));
    }

    @Override
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

    @Override
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
                    if (all.isEmpty()) {
                        return "";
                    }
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

    @Override
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

    @Override
    public Mono<Void> updateCommitStatus(PRUrl prUrl, String sha, String state,
                                          String description, String targetUrl) {
        GitHubCommitStatusReqDTO body = new GitHubCommitStatusReqDTO();
        body.setState(state);
        body.setDescription(description);
        body.setContext("pullcat/code-review");
        body.setTargetUrl(targetUrl);

        return webClient.post()
                .uri("/repos/{owner}/{repo}/statuses/{sha}", prUrl.owner(), prUrl.repo(), sha)
                .bodyValue(body)
                .retrieve()
                .bodyToMono(Void.class);
    }

    @Override
    public Mono<String> fetchHeadSha(PRUrl prUrl) {
        return webClient.get()
                .uri("/repos/{owner}/{repo}/pulls/{number}", prUrl.owner(), prUrl.repo(), prUrl.number())
                .retrieve()
                .bodyToMono(GitHubPullRequestRespDTO.class)
                .map(response -> response.getHead() != null && response.getHead().getSha() != null
                        ? response.getHead().getSha() : "");
    }

    @Override
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

    @Override
    public Mono<String> fetchDiff(PRUrl prUrl) {
        return webClient.get()
                .uri("/repos/{owner}/{repo}/pulls/{number}", prUrl.owner(), prUrl.repo(), prUrl.number())
                .header("Accept", "application/vnd.github.v3.diff")
                .retrieve()
                .bodyToMono(String.class);
    }

    @Override
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

    @Override
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

    @Override
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

    @Override
    public Mono<GitHubApiService> withInstallationToken(long installationId) {
        if (gitHubInstallationTokenService == null) {
            return Mono.just(this);
        }
        return gitHubInstallationTokenService.getInstallationToken(installationId)
                .map(token -> {
                    WebClient installationWebClient = WebClient.builder()
                            .baseUrl("https://api.github.com")
                            .defaultHeader("Accept", "application/vnd.github.v3+json")
                            .defaultHeader("User-Agent", "pullcat")
                            .defaultHeader("Authorization", "Bearer " + token)
                            .filter(forbiddenHandler())
                            .clientConnector(new org.springframework.http.client.reactive.ReactorClientHttpConnector(
                                    HttpClient.create().responseTimeout(Duration.ofSeconds(30))))
                            .build();
                    return (GitHubApiService) new GitHubApiServiceImpl(installationWebClient, meterRegistry);
                });
    }

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

    @Override
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

    @Override
    public Mono<Long> publishReview(PRUrl prUrl, String summaryBody) {
        return publishReviewWithComments(prUrl, summaryBody, List.of());
    }

    @Override
    public Mono<Long> postIssueComment(PRUrl prUrl, String body) {
        return webClient.post()
                .uri("/repos/{owner}/{repo}/issues/{number}/comments",
                        prUrl.owner(), prUrl.repo(), prUrl.number())
                .bodyValue(Map.of("body", body))
                .retrieve()
                .bodyToMono(GitHubCommentRespDTO.class)
                .map(GitHubCommentRespDTO::getId);
    }

    private void recordApiCall(String endpoint, String status) {
        if (meterRegistry != null) {
            meterRegistry.counter("github_api_calls_total", "endpoint", endpoint, "status", status).increment();
        }
    }
}
