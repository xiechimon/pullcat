package com.pullcat.service.github;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pullcat.model.PRMetadata;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GitHubApiServiceTest {

    private static final ObjectMapper mapper = new ObjectMapper();

    private final GitHubApiService service = new GitHubApiService(
            WebClientSupport.noopClient());

    @Test
    void parsePrUrlValid() {
        GitHubApiService.PRUrl pr = service.parsePrUrl(
                "https://github.com/spring-projects/spring-boot/pull/12345");

        assertThat(pr.owner()).isEqualTo("spring-projects");
        assertThat(pr.repo()).isEqualTo("spring-boot");
        assertThat(pr.number()).isEqualTo(12345);
    }

    @Test
    void parsePrUrlWithTrailingPath() {
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

    @Test
    void mapToPRMetadataAllFields() throws Exception {
        JsonNode json = mapper.readTree("""
                {
                  "title": "Add login feature",
                  "body": "Implements user authentication",
                  "base": {"ref": "main"},
                  "head": {"ref": "feature/login"},
                  "changed_files": 12,
                  "additions": 340,
                  "deletions": 55
                }
                """);

        GitHubApiService.PRUrl prUrl = new GitHubApiService.PRUrl("owner", "repo", 100);
        PRMetadata meta = service.mapToPRMetadata(prUrl, json);

        assertThat(meta.getOwner()).isEqualTo("owner");
        assertThat(meta.getRepo()).isEqualTo("repo");
        assertThat(meta.getPullNumber()).isEqualTo(100);
        assertThat(meta.getTitle()).isEqualTo("Add login feature");
        assertThat(meta.getDescription()).isEqualTo("Implements user authentication");
        assertThat(meta.getBaseBranch()).isEqualTo("main");
        assertThat(meta.getHeadBranch()).isEqualTo("feature/login");
        assertThat(meta.getFileCount()).isEqualTo(12);
        assertThat(meta.getAdditions()).isEqualTo(340);
        assertThat(meta.getDeletions()).isEqualTo(55);
    }

    @Test
    void mapToPRMetadataHandlesEmptyResponse() throws Exception {
        JsonNode json = mapper.readTree("{}");
        GitHubApiService.PRUrl prUrl = new GitHubApiService.PRUrl("owner", "repo", 1);

        PRMetadata meta = service.mapToPRMetadata(prUrl, json);

        assertThat(meta.getTitle()).isEmpty();
        assertThat(meta.getDescription()).isEmpty();
        assertThat(meta.getBaseBranch()).isEmpty();
        assertThat(meta.getHeadBranch()).isEmpty();
        assertThat(meta.getFileCount()).isEqualTo(0);
        assertThat(meta.getAdditions()).isEqualTo(0);
        assertThat(meta.getDeletions()).isEqualTo(0);
    }

    @Test
    void mapToPRMetadataPartialResponse() throws Exception {
        JsonNode json = mapper.readTree("""
                {"title": "Fix bug", "additions": 10}
                """);
        GitHubApiService.PRUrl prUrl = new GitHubApiService.PRUrl("a", "b", 1);

        PRMetadata meta = service.mapToPRMetadata(prUrl, json);

        assertThat(meta.getTitle()).isEqualTo("Fix bug");
        assertThat(meta.getAdditions()).isEqualTo(10);
        assertThat(meta.getDeletions()).isEqualTo(0);
        assertThat(meta.getDescription()).isEmpty();
    }
}
