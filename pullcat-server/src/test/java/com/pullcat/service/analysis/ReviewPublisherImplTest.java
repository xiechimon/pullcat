package com.pullcat.service.analysis;

import com.pullcat.common.enums.AnalysisType;
import com.pullcat.common.enums.SessionStatus;
import com.pullcat.common.enums.Severity;
import com.pullcat.dto.resp.AnalysisResultRespDTO;
import com.pullcat.dto.resp.BooleanStatusRespDTO;
import com.pullcat.dto.resp.IssueRespDTO;
import com.pullcat.dto.resp.ReviewSessionRespDTO;
import com.pullcat.remote.GitHubApiService;
import com.pullcat.service.AutoPublishService;
import com.pullcat.service.analysis.impl.ReviewPublisherImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import reactor.core.publisher.Mono;

import java.util.LinkedHashMap;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReviewPublisherImplTest {

    @Mock
    private GitHubApiService gitHubApiService;

    @Mock
    private ReviewSessionService reviewSessionService;

    @Mock
    private ResultAggregator resultAggregator;

    @Mock
    private AutoPublishService autoPublishService;

    @InjectMocks
    private ReviewPublisherImpl reviewPublisher;

    @Test
    void publishReview_embedsDetailLinkAndDeduplicatesSummaryAndComments() {
        ReflectionTestUtils.setField(reviewPublisher, "baseUrl", "https://pullcat.example.com");

        ReviewSessionRespDTO session = session("review-123");
        IssueRespDTO first = issue(
                "README.md 包含无意义的占位内容",
                "README.md 包含无意义的占位内容",
                Severity.HIGH,
                "README.md",
                1,
                "new readme"
        );
        IssueRespDTO second = issue(
                "README.md包含无意义的占位符内容",
                "README.md 包含无意义的占位符内容",
                Severity.MEDIUM,
                "README.md",
                1,
                "better readme"
        );

        when(reviewSessionService.findById("review-123")).thenReturn(session);
        when(resultAggregator.mergeResults(anyList())).thenReturn(List.of(first, second));
        when(gitHubApiService.parsePrUrl(session.getPrUrl())).thenReturn(new GitHubApiService.PRUrl("octo", "repo", 7));
        when(gitHubApiService.publishReviewWithComments(
                eq(new GitHubApiService.PRUrl("octo", "repo", 7)), anyString(), anyList()))
                .thenReturn(Mono.just(99L));

        reviewPublisher.publishReview("review-123");

        ArgumentCaptor<String> summaryCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<List<GitHubApiService.ReviewComment>> commentCaptor = ArgumentCaptor.forClass(List.class);
        verify(gitHubApiService).publishReviewWithComments(
                eq(new GitHubApiService.PRUrl("octo", "repo", 7)),
                summaryCaptor.capture(),
                commentCaptor.capture()
        );

        String summary = summaryCaptor.getValue();
        assertTrue(summary.contains("### 审查详情"));
        assertTrue(summary.contains("https://pullcat.example.com/review/review-123"));
        assertTrue(summary.contains("问题概览（1 个）"));

        List<GitHubApiService.ReviewComment> comments = commentCaptor.getValue();
        assertEquals(1, comments.size());
        assertEquals("README.md", comments.get(0).file());

        verify(reviewSessionService).save(session);
        assertEquals(SessionStatus.PUBLISHED, session.getStatus());
        assertEquals(99L, session.getPublishedCommentId());
    }

    @Test
    void buildPublishSummary_usesFallbackBaseUrlWhenConfigMissing() {
        ReflectionTestUtils.setField(reviewPublisher, "baseUrl", "");

        ReviewSessionRespDTO session = session("review-456");
        String summary = ReflectionTestUtils.invokeMethod(reviewPublisher, "buildPublishSummary", List.of(), session);

        assertTrue(summary.contains("http://localhost:5173/review/review-456"));
        assertTrue(summary.contains("PR 链接：https://github.com/octo/repo/pull/7"));
    }

    @Test
    void tryAutoPublish_keepsPublishSummaryDeduplicated() {
        ReflectionTestUtils.setField(reviewPublisher, "baseUrl", "https://pullcat.example.com");

        ReviewSessionRespDTO session = session("review-789");
        session.setRepositoryFullName("octo/repo");
        IssueRespDTO first = issue("重复问题", "详情 A", Severity.MEDIUM, "README.md", 2, null);
        IssueRespDTO second = issue("重复 问题", "详情更长的 B", Severity.HIGH, "README.md", 2, null);

        when(autoPublishService.getStatus("octo", "repo")).thenReturn(new BooleanStatusRespDTO(true));
        when(resultAggregator.mergeResults(anyList())).thenReturn(List.of(first, second));
        when(gitHubApiService.parsePrUrl(session.getPrUrl())).thenReturn(new GitHubApiService.PRUrl("octo", "repo", 7));
        when(gitHubApiService.publishReview(eq(new GitHubApiService.PRUrl("octo", "repo", 7)), anyString()))
                .thenReturn(Mono.just(100L));

        boolean published = reviewPublisher.tryAutoPublish(session);

        assertTrue(published);

        ArgumentCaptor<String> summaryCaptor = ArgumentCaptor.forClass(String.class);
        verify(gitHubApiService).publishReview(eq(new GitHubApiService.PRUrl("octo", "repo", 7)), summaryCaptor.capture());
        assertTrue(summaryCaptor.getValue().contains("问题概览（1 个）"));
    }

    private ReviewSessionRespDTO session(String id) {
        ReviewSessionRespDTO session = new ReviewSessionRespDTO();
        session.setId(id);
        session.setPrUrl("https://github.com/octo/repo/pull/7");
        session.setRepositoryFullName("octo/repo");

        AnalysisResultRespDTO summary = new AnalysisResultRespDTO(AnalysisType.SUMMARY);
        summary.setContent("{\"summary\":\"这里是审查详情摘要\"}");
        session.setAnalyses(new LinkedHashMap<>(java.util.Map.of("summary", summary)));
        return session;
    }

    private IssueRespDTO issue(String title, String description, Severity severity, String file, int line, String suggestionCode) {
        IssueRespDTO issue = new IssueRespDTO();
        issue.setTitle(title);
        issue.setDescription(description);
        issue.setSeverity(severity);
        issue.setFile(file);
        issue.setLine(line);
        issue.setSuggestionCode(suggestionCode == null ? "fixed content" : suggestionCode);
        return issue;
    }
}
