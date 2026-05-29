package com.pullcat.service.github;

import com.pullcat.config.GitHubConfig;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GitHubApiServiceTest {

    private final GitHubApiService service = new GitHubApiService(
            new DummyGitHubConfig("dummy-token"));

    static class DummyGitHubConfig extends GitHubConfig {
        DummyGitHubConfig(String token) {
            setToken(token);
        }
    }

    @Test
    void parsePrUrlValid() {
        GitHubApiService.PRUrl pr = service.parsePrUrl(
                "https://github.com/spring-projects/spring-boot/pull/12345");

        assertThat(pr.owner()).isEqualTo("spring-projects");
        assertThat(pr.repo()).isEqualTo("spring-boot");
        assertThat(pr.number()).isEqualTo(12345);
    }

    @Test
    void parsePrUrlWithTrailingSlash() {
        GitHubApiService.PRUrl pr = service.parsePrUrl(
                "https://github.com/owner/repo/pull/42/files");

        assertThat(pr.owner()).isEqualTo("owner");
        assertThat(pr.repo()).isEqualTo("repo");
        assertThat(pr.number()).isEqualTo(42);
    }

    @Test
    void parsePrUrlInvalidHost() {
        assertThatThrownBy(() -> service.parsePrUrl("https://gitlab.com/owner/repo/pull/1"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid GitHub PR URL");
    }

    @Test
    void parsePrUrlInvalidPath() {
        assertThatThrownBy(() -> service.parsePrUrl("https://github.com/owner/repo/issues/1"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid GitHub PR URL");
    }

    @Test
    void parsePrUrlRandomString() {
        assertThatThrownBy(() -> service.parsePrUrl("not a url"))
                .isInstanceOf(IllegalArgumentException.class);
    }

}
