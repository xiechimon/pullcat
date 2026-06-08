package com.pullcat.service.analysis;

import com.pullcat.common.enums.AnalysisStatus;
import com.pullcat.common.enums.AnalysisType;
import com.pullcat.common.enums.SessionStatus;
import com.pullcat.common.enums.Severity;
import com.pullcat.dao.entity.RuleDO;
import com.pullcat.dto.req.PublishReqDTO;
import com.pullcat.dto.resp.AnalysisResultRespDTO;
import com.pullcat.dto.resp.CommonIssueTypeRespDTO;
import com.pullcat.dto.resp.FileContentRespDTO;
import com.pullcat.dto.resp.IssueRespDTO;
import com.pullcat.dto.resp.PRDataRespDTO;
import com.pullcat.dto.resp.PRMetadataRespDTO;
import com.pullcat.dto.resp.RepoStatsRespDTO;
import com.pullcat.dto.resp.ReviewSessionRespDTO;
import com.pullcat.dto.resp.StatsOverviewRespDTO;
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
        ReviewSessionRespDTO s1 = new ReviewSessionRespDTO();
        s1.setRepositoryFullName("owner/repo1");
        AnalysisResultRespDTO r1 = new AnalysisResultRespDTO();
        r1.setIssues(List.of(
                createIssue(Severity.CRITICAL, "NPE risk"),
                createIssue(Severity.HIGH, "Race condition"),
                createIssue(Severity.MEDIUM, "Missing validation")
        ));
        s1.getAnalyses().put("risk", r1);

        ReviewSessionRespDTO s2 = new ReviewSessionRespDTO();
        s2.setRepositoryFullName("owner/repo2");
        AnalysisResultRespDTO r2 = new AnalysisResultRespDTO();
        r2.setIssues(List.of(
                createIssue(Severity.CRITICAL, "SQL injection"),
                createIssue(Severity.LOW, "Missing javadoc")
        ));
        s2.getAnalyses().put("risk", r2);

        when(reviewRepository.findAllReviews()).thenReturn(List.of(s1, s2));

        var overview = statsService.getOverview();

        assertThat(overview.getTotalReviews()).isEqualTo(2);
        assertThat(overview.getTotalIssues()).isEqualTo(5);
        assertThat(overview.getRepoCount()).isEqualTo(2);
        assertThat(overview.getAvgIssuesPerReview()).isEqualTo(2.5);

        var severityDist = overview.getSeverityDistribution();
        assertThat(severityDist.get("CRITICAL")).isEqualTo(2);
        assertThat(severityDist.get("HIGH")).isEqualTo(1);
        assertThat(severityDist.get("MEDIUM")).isEqualTo(1);
        assertThat(severityDist.get("LOW")).isEqualTo(1);
        assertThat(overview.getCommonIssueTypes()).isNotNull().allMatch(item -> item instanceof CommonIssueTypeRespDTO);
    }

    @Test
    void getOverviewNoReviews() {
        when(reviewRepository.findAllReviews()).thenReturn(List.of());

        var overview = statsService.getOverview();

        assertThat(overview.getTotalReviews()).isEqualTo(0);
        assertThat(overview.getTotalIssues()).isEqualTo(0);
        assertThat(overview.getRepoCount()).isEqualTo(0);
        assertThat(overview.getAvgIssuesPerReview()).isEqualTo(0.0);
    }

    @Test
    void getRepoStatsWithReviews() {
        ReviewSessionRespDTO s1 = new ReviewSessionRespDTO();
        s1.setRepositoryFullName("owner/myrepo");
        AnalysisResultRespDTO r1 = new AnalysisResultRespDTO();
        r1.setIssues(List.of(
                createIssue(Severity.HIGH, "IssueRespDTO 1"),
                createIssue(Severity.HIGH, "IssueRespDTO 2")
        ));
        s1.getAnalyses().put("risk", r1);

        when(reviewRepository.findByRepo("owner/myrepo", 0, Integer.MAX_VALUE))
                .thenReturn(List.of(s1));

        var stats = statsService.getRepoStats("owner", "myrepo");

        assertThat(stats.getTotalReviews()).isEqualTo(1);
        assertThat(stats.getTotalIssues()).isEqualTo(2);
        assertThat(stats.getRepoFullName()).isEqualTo("owner/myrepo");
    }

    @Test
    void getRepoStatsNoReviews() {
        when(reviewRepository.findByRepo("owner/empty", 0, Integer.MAX_VALUE))
                .thenReturn(List.of());

        var stats = statsService.getRepoStats("owner", "empty");

        assertThat(stats.getTotalReviews()).isEqualTo(0);
        assertThat(stats.getTotalIssues()).isEqualTo(0);
    }

    private IssueRespDTO createIssue(Severity severity, String title) {
        IssueRespDTO issue = new IssueRespDTO();
        issue.setSeverity(severity);
        issue.setTitle(title);
        return issue;
    }
}
