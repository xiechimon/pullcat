package com.pullcat.service.github;

import com.fasterxml.jackson.databind.JsonNode;
import com.pullcat.config.GitHubConfig;
import com.pullcat.model.PRMetadata;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * GitHub API 服务，封装与 GitHub REST API 的交互逻辑。
 * <p>
 * 提供 PR URL 解析以及 PR 元数据拉取功能。所有 API 调用均以响应式（Mono）方式返回。
 */
@Slf4j
@Service
public class GitHubApiService {

    private static final Pattern PR_URL_PATTERN =
            Pattern.compile("https?://github\\.com/([^/]+)/([^/]+)/pull/(\\d+).*");

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
     * 解析后的 GitHub PR URL 信息。
     */
    public record PRUrl(String owner, String repo, int number) {

    }
}
