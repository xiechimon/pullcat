package com.pullcat.controller;

import com.pullcat.common.convention.result.Result;
import com.pullcat.dto.req.WebhookEventReqDTO;
import com.pullcat.dto.req.WebhookPullRequestReqDTO;
import com.pullcat.dto.resp.WebhookRespDTO;
import com.pullcat.service.analysis.WebhookService;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class WebhookControllerTest {

    private final WebhookService webhookService = mock(WebhookService.class);
    private final WebhookController controller = new WebhookController(webhookService);

    @Test
    void handlePrOpenedEvent() {
        WebhookEventReqDTO payload = createPayload("opened", "https://github.com/owner/repo/pull/1");

        ResponseEntity<Result<WebhookRespDTO>> response = controller.handleGitHubWebhook("pull_request", payload);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isSuccess()).isTrue();
        assertThat(response.getBody().getData().getStatus()).isEqualTo("review_triggered");
        verify(webhookService).triggerReview("https://github.com/owner/repo/pull/1");
    }

    @Test
    void handlePrSynchronizeEvent() {
        WebhookEventReqDTO payload = createPayload("synchronize", "https://github.com/owner/repo/pull/2");

        ResponseEntity<Result<WebhookRespDTO>> response = controller.handleGitHubWebhook("pull_request", payload);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getData().getStatus()).isEqualTo("review_triggered");
    }

    @Test
    void handleNonPrEvent() {
        ResponseEntity<Result<WebhookRespDTO>> response = controller.handleGitHubWebhook("push", new WebhookEventReqDTO());

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getData().getStatus()).isEqualTo("ignored");
        verifyNoInteractions(webhookService);
    }

    @Test
    void handlePrClosedEvent() {
        WebhookEventReqDTO payload = new WebhookEventReqDTO();
        payload.setAction("closed");

        ResponseEntity<Result<WebhookRespDTO>> response = controller.handleGitHubWebhook("pull_request", payload);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getData().getStatus()).isEqualTo("ignored");
        verifyNoInteractions(webhookService);
    }

    private WebhookEventReqDTO createPayload(String action, String htmlUrl) {
        WebhookPullRequestReqDTO pullRequest = new WebhookPullRequestReqDTO();
        pullRequest.setHtmlUrl(htmlUrl);

        WebhookEventReqDTO payload = new WebhookEventReqDTO();
        payload.setAction(action);
        payload.setPullRequest(pullRequest);
        return payload;
    }
}
