package com.pullcat.service.analysis;

import com.pullcat.model.*;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class StatsServiceTest {

    private final ReviewRepository reviewRepository = mock(ReviewRepository.class);
    private final StatsService statsService = new StatsService(reviewRepository);

    @Test
    void getOverviewWithReviews() {
        ReviewSession s1 = new ReviewSession();
        s1.setRepositoryFullName("owner/repo1");
        AnalysisResult r1 = new AnalysisResult();
        r1.setIssues(List.of(
                createIssue(Issue.Severity.CRITICAL, "NPE risk"),
                createIssue(Issue.Severity.HIGH, "Race condition"),
                createIssue(Issue.Severity.MEDIUM, "Missing validation")
        ));
        s1.getAnalyses().put("risk", r1);

        ReviewSession s2 = new ReviewSession();
        s2.setRepositoryFullName("owner/repo2");
        AnalysisResult r2 = new AnalysisResult();
        r2.setIssues(List.of(
                createIssue(Issue.Severity.CRITICAL, "SQL injection"),
                createIssue(Issue.Severity.LOW, "Missing javadoc")
        ));
        s2.getAnalyses().put("risk", r2);

        when(reviewRepository.findAllReviews()).thenReturn(List.of(s1, s2));

        var overview = statsService.getOverview();

        assertThat(overview.get("totalReviews")).isEqualTo(2);
        assertThat(overview.get("totalIssues")).isEqualTo(5);
        assertThat(overview.get("repoCount")).isEqualTo(2);
        assertThat((Double) overview.get("avgIssuesPerReview")).isEqualTo(2.5);

        @SuppressWarnings("unchecked")
        var severityDist = (java.util.Map<String, Integer>) overview.get("severityDistribution");
        assertThat(severityDist.get("CRITICAL")).isEqualTo(2);
        assertThat(severityDist.get("HIGH")).isEqualTo(1);
        assertThat(severityDist.get("MEDIUM")).isEqualTo(1);
        assertThat(severityDist.get("LOW")).isEqualTo(1);
    }

    @Test
    void getOverviewNoReviews() {
        when(reviewRepository.findAllReviews()).thenReturn(List.of());

        var overview = statsService.getOverview();

        assertThat(overview.get("totalReviews")).isEqualTo(0);
        assertThat(overview.get("totalIssues")).isEqualTo(0);
        assertThat(overview.get("repoCount")).isEqualTo(0);
        assertThat((Double) overview.get("avgIssuesPerReview")).isEqualTo(0.0);
    }

    @Test
    void getRepoStatsWithReviews() {
        ReviewSession s1 = new ReviewSession();
        s1.setRepositoryFullName("owner/myrepo");
        AnalysisResult r1 = new AnalysisResult();
        r1.setIssues(List.of(
                createIssue(Issue.Severity.HIGH, "Issue 1"),
                createIssue(Issue.Severity.HIGH, "Issue 2")
        ));
        s1.getAnalyses().put("risk", r1);

        when(reviewRepository.findByRepo("owner/myrepo", 0, Integer.MAX_VALUE))
                .thenReturn(List.of(s1));

        var stats = statsService.getRepoStats("owner", "myrepo");

        assertThat(stats.get("totalReviews")).isEqualTo(1);
        assertThat(stats.get("totalIssues")).isEqualTo(2);
        assertThat(stats.get("repoFullName")).isEqualTo("owner/myrepo");
    }

    @Test
    void getRepoStatsNoReviews() {
        when(reviewRepository.findByRepo("owner/empty", 0, Integer.MAX_VALUE))
                .thenReturn(List.of());

        var stats = statsService.getRepoStats("owner", "empty");

        assertThat(stats.get("totalReviews")).isEqualTo(0);
        assertThat(stats.get("totalIssues")).isEqualTo(0);
    }

    private Issue createIssue(Issue.Severity severity, String title) {
        Issue issue = new Issue();
        issue.setSeverity(severity);
        issue.setTitle(title);
        return issue;
    }
}
