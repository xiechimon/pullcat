package com.pullcat.service.analysis;

import com.pullcat.model.ReviewSession;
import org.junit.jupiter.api.Test;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class WebhookServiceTest {

    private final AnalysisOrchestrator orchestrator = mock(AnalysisOrchestrator.class);
    private final ReviewRepository reviewRepository = mock(ReviewRepository.class);
    private final WebhookService webhookService = new WebhookService(orchestrator, reviewRepository);

    @Test
    void triggerReviewCreatesSessionAndStartsAsync() {
        ReviewSession session = new ReviewSession();
        session.setId("test-session");
        session.setPrUrl("https://github.com/owner/repo/pull/1");

        when(orchestrator.createSession(anyString(), any())).thenReturn(session);

        webhookService.triggerReview("https://github.com/owner/repo/pull/1");

        verify(orchestrator).createSession(eq("https://github.com/owner/repo/pull/1"), eq((String) null));
        verify(reviewRepository).save(session);
        verify(orchestrator).startReviewAsync(session);
    }
}
