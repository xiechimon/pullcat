package com.pullcat.service.analysis;

import com.pullcat.model.*;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CompareServiceTest {

    private final ReviewRepository reviewRepository = mock(ReviewRepository.class);
    private final CompareService compareService = new CompareService(reviewRepository);

    @Test
    void compareTwoReviewsWithNewAndFixedIssues() {
        ReviewSession r1 = createSession("r1", "https://github.com/a/b/pull/1",
                createIssue("src/Foo.java", 10, "NPE risk"),
                createIssue("src/Bar.java", 20, "Missing validation"));

        ReviewSession r2 = createSession("r2", "https://github.com/a/b/pull/2",
                createIssue("src/Foo.java", 10, "NPE risk"),
                createIssue("src/Baz.java", 30, "SQL injection"));

        when(reviewRepository.findById("r1")).thenReturn(r1);
        when(reviewRepository.findById("r2")).thenReturn(r2);

        Map<String, Object> result = compareService.compare("r1", "r2");

        assertThat(result.get("newCount")).isEqualTo(1);
        assertThat(result.get("fixedCount")).isEqualTo(1);
        assertThat(result.get("persistentCount")).isEqualTo(1);
        assertThat(result.get("totalIssues1")).isEqualTo(2);
        assertThat(result.get("totalIssues2")).isEqualTo(2);
    }

    @Test
    void compareWhenReviewNotFound() {
        when(reviewRepository.findById("r1")).thenReturn(null);
        when(reviewRepository.findById("r2")).thenReturn(createSession("r2", ""));

        Map<String, Object> result = compareService.compare("r1", "r2");

        assertThat(result).containsKey("error");
    }

    @Test
    void compareIdenticalReviews() {
        ReviewSession session = createSession("r1", "https://github.com/a/b/pull/1",
                createIssue("src/Foo.java", 10, "Same issue"));

        when(reviewRepository.findById("r1")).thenReturn(session);
        when(reviewRepository.findById("r2")).thenReturn(session);

        Map<String, Object> result = compareService.compare("r1", "r2");

        assertThat(result.get("newCount")).isEqualTo(0);
        assertThat(result.get("fixedCount")).isEqualTo(0);
        assertThat(result.get("persistentCount")).isEqualTo(1);
    }

    private ReviewSession createSession(String id, String prUrl, Issue... issues) {
        ReviewSession session = new ReviewSession();
        session.setId(id);
        session.setPrUrl(prUrl);

        AnalysisResult result = new AnalysisResult();
        result.setType(AnalysisType.RISK);
        result.setIssues(List.of(issues));
        session.getAnalyses().put("risk", result);

        return session;
    }

    private Issue createIssue(String file, int line, String title) {
        Issue issue = new Issue();
        issue.setFile(file);
        issue.setLine(line);
        issue.setTitle(title);
        return issue;
    }
}
