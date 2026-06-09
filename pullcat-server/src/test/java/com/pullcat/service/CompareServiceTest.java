package com.pullcat.service;

import com.pullcat.common.convention.exception.ClientException;
import com.pullcat.dto.resp.AnalysisResultRespDTO;
import com.pullcat.dto.resp.CompareReviewsRespDTO;
import com.pullcat.dto.resp.IssueRespDTO;
import com.pullcat.dto.resp.ReviewSessionRespDTO;
import com.pullcat.service.analysis.ReviewSessionService;
import com.pullcat.service.impl.CompareServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.LinkedHashMap;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CompareServiceTest {

    @Mock
    private ReviewSessionService reviewSessionService;

    @InjectMocks
    private CompareServiceImpl compareService;

    @Test
    void compare_nullReviewIds_throwsClientException() {
        assertThrows(ClientException.class, () -> compareService.compare(null));
    }

    @Test
    void compare_singleReviewId_throwsClientException() {
        assertThrows(ClientException.class, () -> compareService.compare(List.of("r1")));
    }

    @Test
    void compare_moreThanTwoReviewIds_throwsClientException() {
        assertThrows(ClientException.class, () -> compareService.compare(List.of("r1", "r2", "r3")));
    }

    @Test
    void compare_missingReview_throwsClientExceptionWithExpectedMessage() {
        when(reviewSessionService.findById("r1")).thenReturn(review("https://github.com/o/r/pull/1", issue("A.java", 10, "same issue")));
        when(reviewSessionService.findById("r2")).thenReturn(null);

        ClientException exception = assertThrows(ClientException.class, () -> compareService.compare("r1", "r2"));

        assertEquals("审查记录不存在", exception.getMessage());
    }

    @Test
    void compare_twoReviewIds_delegatesToNormalComparison() {
        when(reviewSessionService.findById("r1")).thenReturn(review("https://github.com/o/r/pull/1", issue("A.java", 10, "same issue")));
        when(reviewSessionService.findById("r2")).thenReturn(review(
                "https://github.com/o/r/pull/2",
                issue("A.java", 10, "same issue"),
                issue("B.java", 20, "new issue")
        ));

        CompareReviewsRespDTO result = compareService.compare(List.of("r1", "r2"));

        assertEquals("r1", result.getReview1().getId());
        assertEquals("r2", result.getReview2().getId());
        assertEquals(1, result.getNewCount());
        assertEquals(0, result.getFixedCount());
        assertEquals(1, result.getPersistentCount());
        assertEquals(1, result.getTotalIssues1());
        assertEquals(2, result.getTotalIssues2());
    }

    @Test
    void compare_twoReviewIdsByString_returnsComparisonResult() {
        when(reviewSessionService.findById("r1")).thenReturn(review("https://github.com/o/r/pull/1", issue("A.java", 10, "same issue")));
        when(reviewSessionService.findById("r2")).thenReturn(review(
                "https://github.com/o/r/pull/2",
                issue("A.java", 10, "same issue"),
                issue("B.java", 20, "new issue")
        ));

        CompareReviewsRespDTO result = compareService.compare("r1", "r2");

        assertEquals("r1", result.getReview1().getId());
        assertEquals("r2", result.getReview2().getId());
        assertEquals(1, result.getNewCount());
        assertEquals(0, result.getFixedCount());
        assertEquals(1, result.getPersistentCount());
        assertEquals(1, result.getTotalIssues1());
        assertEquals(2, result.getTotalIssues2());
    }

    private ReviewSessionRespDTO review(String prUrl, IssueRespDTO... issues) {
        AnalysisResultRespDTO analysis = new AnalysisResultRespDTO();
        analysis.setIssues(List.of(issues));

        ReviewSessionRespDTO session = new ReviewSessionRespDTO();
        session.setPrUrl(prUrl);
        session.setAnalyses(new LinkedHashMap<>(java.util.Map.of("summary", analysis)));
        return session;
    }

    private IssueRespDTO issue(String file, int line, String title) {
        IssueRespDTO issue = new IssueRespDTO();
        issue.setFile(file);
        issue.setLine(line);
        issue.setTitle(title);
        return issue;
    }
}
