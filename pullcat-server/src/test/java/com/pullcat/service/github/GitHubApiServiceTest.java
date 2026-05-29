package com.pullcat.service.github;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pullcat.model.PRMetadata;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

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

    @ParameterizedTest
    @ValueSource(strings = {
            "image.png", "photo.jpg", "icon.svg", "doc.pdf",
            "archive.zip", "binary.exe", "font.ttf",
            "lib.jar", "data.db", "package-lock.json", "yarn.lock"
    })
    void shouldExcludeBinaryFiles(String filename) throws Exception {
        assertThat(invokeShouldExclude(filename)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "src/main/Main.java", "README.md", "build.gradle",
            "pom.xml", "src/index.ts", "test.py", "main.go"
    })
    void shouldNotExcludeSourceFiles(String filename) throws Exception {
        assertThat(invokeShouldExclude(filename)).isFalse();
    }

    @Test
    void shouldExcludeGeneratedPaths() throws Exception {
        assertThat(invokeShouldExclude("target/classes/Foo.class")).isTrue();
        assertThat(invokeShouldExclude("node_modules/lodash/index.js")).isTrue();
        assertThat(invokeShouldExclude("dist/bundle.js")).isTrue();
        assertThat(invokeShouldExclude("build/output.txt")).isTrue();
    }

    @Test
    void shouldExcludeDotFiles() throws Exception {
        assertThat(invokeShouldExclude(".gitignore")).isTrue();
        assertThat(invokeShouldExclude(".env")).isTrue();
    }

    private boolean invokeShouldExclude(String filename) throws Exception {
        Method method = GitHubApiService.class.getDeclaredMethod("shouldExcludeFile", String.class);
        method.setAccessible(true);
        return (boolean) method.invoke(service, filename);
    }

    @Test
    void reviewCommentRecord() {
        GitHubApiService.ReviewComment comment = new GitHubApiService.ReviewComment(
                "src/main/Foo.java", 42, "可能存在空指针");

        assertThat(comment.file()).isEqualTo("src/main/Foo.java");
        assertThat(comment.line()).isEqualTo(42);
        assertThat(comment.body()).isEqualTo("可能存在空指针");
    }

    @Test
    void buildReviewBodyWithoutComments() {
        Map<String, Object> body = service.buildReviewBody("## AI Review\n\nLGTM", List.of());

        assertThat(body.get("event")).isEqualTo("COMMENT");
        assertThat(body.get("body")).isEqualTo("## AI Review\n\nLGTM");
        assertThat(body.containsKey("comments")).isFalse();
    }

    @Test
    @SuppressWarnings("unchecked")
    void buildReviewBodyWithComments() {
        List<GitHubApiService.ReviewComment> comments = List.of(
                new GitHubApiService.ReviewComment("src/Foo.java", 10, "NPE risk"),
                new GitHubApiService.ReviewComment("src/Bar.java", 20, "missing validation")
        );

        Map<String, Object> body = service.buildReviewBody("## Summary", comments);

        assertThat(body.get("event")).isEqualTo("COMMENT");
        assertThat(body.containsKey("comments")).isTrue();

        List<Map<String, Object>> commentList = (List<Map<String, Object>>) body.get("comments");
        assertThat(commentList).hasSize(2);
        assertThat(commentList.get(0).get("path")).isEqualTo("src/Foo.java");
        assertThat(commentList.get(0).get("line")).isEqualTo(10);
        assertThat(commentList.get(0).get("side")).isEqualTo("RIGHT");
        assertThat(commentList.get(0).get("body")).isEqualTo("NPE risk");
    }

    @Test
    @SuppressWarnings("unchecked")
    void buildReviewBodyFiltersNullFileAndLine() {
        List<GitHubApiService.ReviewComment> comments = List.of(
                new GitHubApiService.ReviewComment(null, 10, "no file"),
                new GitHubApiService.ReviewComment("src/Foo.java", null, "no line"),
                new GitHubApiService.ReviewComment("src/Valid.java", 30, "valid")
        );

        Map<String, Object> body = service.buildReviewBody("## Summary", comments);

        List<Map<String, Object>> commentList = (List<Map<String, Object>>) body.get("comments");
        assertThat(commentList).hasSize(1);
        assertThat(commentList.get(0).get("path")).isEqualTo("src/Valid.java");
    }
}
