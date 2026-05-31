package com.pullcat.controller;

import com.pullcat.service.analysis.WebhookService;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class WebhookControllerTest {

    private final WebhookService webhookService = mock(WebhookService.class);
    private final WebhookController controller = new WebhookController(webhookService);

    @Test
    void handlePrOpenedEvent() {
        Map<String, Object> payload = Map.of(
                "action", "opened",
                "pull_request", Map.of("html_url", "https://github.com/owner/repo/pull/1")
        );

        ResponseEntity<Map<String, Object>> response = controller.handleGitHubWebhook("pull_request", payload);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).containsEntry("status", "review_triggered");
        verify(webhookService).triggerReview("https://github.com/owner/repo/pull/1");
    }

    @Test
    void handlePrSynchronizeEvent() {
        Map<String, Object> payload = Map.of(
                "action", "synchronize",
                "pull_request", Map.of("html_url", "https://github.com/owner/repo/pull/2")
        );

        ResponseEntity<Map<String, Object>> response = controller.handleGitHubWebhook("pull_request", payload);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).containsEntry("status", "review_triggered");
    }

    @Test
    void handleNonPrEvent() {
        ResponseEntity<Map<String, Object>> response = controller.handleGitHubWebhook("push", Map.of());

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).containsEntry("status", "ignored");
        verifyNoInteractions(webhookService);
    }

    @Test
    void handlePrClosedEvent() {
        Map<String, Object> payload = Map.of("action", "closed");

        ResponseEntity<Map<String, Object>> response = controller.handleGitHubWebhook("pull_request", payload);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).containsEntry("status", "ignored");
        verifyNoInteractions(webhookService);
    }
}
