package com.pullcat.service.github;

import com.pullcat.config.GitHubConfig;
import com.pullcat.model.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * GitHub API 服务，封装与 GitHub REST API 的交互逻辑。
 */
@Slf4j
@Service
public class GitHubApiService {

    private static final Pattern PR_URL_PATTERN =
            Pattern.compile("https?://github\\.com/([^/]+)/([^/]+)/pull/(\\d+).*");


    private final WebClient webClient;
    private final String token;

    public GitHubApiService(GitHubConfig config) {
        this.token = config.getToken();
        this.webClient = WebClient.builder()
                .baseUrl("https://api.github.com")
                .defaultHeader("Authorization", "Bearer " + token)
                .defaultHeader("Accept", "application/vnd.github.v3+json")
                .defaultHeader("User-Agent", "pullcat")
                .build();
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


    public record PRUrl(String owner, String repo, int number) {
        public String ref() {
            return "heads/" + number;
        }

        public String headRef() {
            return number > 0 ? String.valueOf(number) : "main";
        }
    }
}
