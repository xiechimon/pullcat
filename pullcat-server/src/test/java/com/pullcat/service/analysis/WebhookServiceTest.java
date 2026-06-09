package com.pullcat.service.analysis;

import com.pullcat.dto.req.WebhookEventReqDTO;
import com.pullcat.dto.req.WebhookPullRequestReqDTO;
import com.pullcat.dto.resp.ReviewSessionRespDTO;
import com.pullcat.dto.resp.WebhookRespDTO;
import com.pullcat.service.impl.WebhookServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WebhookServiceTest {

    @Mock
    AnalysisOrchestrator orchestrator;

    @Mock
    ReviewRepository reviewRepository;

    @InjectMocks
    WebhookServiceImpl webhookService;

    @Test
    void handle_nonPrEvent_returnsIgnored() {
        WebhookRespDTO result = webhookService.handle("push", new WebhookEventReqDTO());
        assertEquals("ignored", result.getStatus());
        assertEquals("not a PR event", result.getReason());
        verifyNoInteractions(orchestrator);
    }

    @Test
    void handle_prEventUnsupportedAction_returnsIgnored() {
        WebhookEventReqDTO req = new WebhookEventReqDTO();
        req.setAction("closed");
        WebhookRespDTO result = webhookService.handle("pull_request", req);
        assertEquals("ignored", result.getStatus());
        assertEquals("closed", result.getAction());
        verifyNoInteractions(orchestrator);
    }

    @Test
    void handle_prOpened_triggersReview() {
        WebhookPullRequestReqDTO pr = new WebhookPullRequestReqDTO();
        pr.setHtmlUrl("https://github.com/a/b/pull/1");
        WebhookEventReqDTO req = new WebhookEventReqDTO();
        req.setAction("opened");
        req.setPullRequest(pr);

        ReviewSessionRespDTO session = new ReviewSessionRespDTO();
        session.setId("s1");
        when(orchestrator.createSession("https://github.com/a/b/pull/1", null)).thenReturn(session);

        WebhookRespDTO result = webhookService.handle("pull_request", req);

        assertEquals("review_triggered", result.getStatus());
        assertEquals("https://github.com/a/b/pull/1", result.getPrUrl());
        verify(orchestrator).startReviewAsync(session);
        verify(reviewRepository).save(session);
    }

    @Test
    void handle_prSynchronize_triggersReview() {
        WebhookPullRequestReqDTO pr = new WebhookPullRequestReqDTO();
        pr.setHtmlUrl("https://github.com/a/b/pull/2");
        WebhookEventReqDTO req = new WebhookEventReqDTO();
        req.setAction("synchronize");
        req.setPullRequest(pr);

        ReviewSessionRespDTO session = new ReviewSessionRespDTO();
        session.setId("s2");
        when(orchestrator.createSession("https://github.com/a/b/pull/2", null)).thenReturn(session);

        WebhookRespDTO result = webhookService.handle("pull_request", req);

        assertEquals("review_triggered", result.getStatus());
        verify(reviewRepository).save(session);
        verify(orchestrator).startReviewAsync(session);
    }
}
