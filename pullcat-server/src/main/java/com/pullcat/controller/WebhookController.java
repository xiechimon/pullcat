package com.pullcat.controller;

import com.pullcat.service.analysis.AnalysisOrchestrator;
import com.pullcat.service.analysis.WebhookService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/webhooks")
public class WebhookController {

    private static final Logger log = LoggerFactory.getLogger(WebhookController.class);

    private final WebhookService webhookService;

    public WebhookController(WebhookService webhookService) {
        this.webhookService = webhookService;
    }

    @PostMapping("/github")
    public ResponseEntity<Map<String, Object>> handleGitHubWebhook(
            @RequestHeader("X-GitHub-Event") String eventType,
            @RequestBody Map<String, Object> payload) {

        log.info("Received webhook event: {}", eventType);

        if (!"pull_request".equals(eventType)) {
            return ResponseEntity.ok(Map.of("status", "ignored", "reason", "not a PR event"));
        }

        String action = (String) payload.get("action");
        if (!"opened".equals(action) && !"synchronize".equals(action)) {
            return ResponseEntity.ok(Map.of("status", "ignored", "action", action));
        }

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> pr = (Map<String, Object>) payload.get("pull_request");
            String prUrl = (String) pr.get("html_url");
            webhookService.triggerReview(prUrl);
            return ResponseEntity.ok(Map.of("status", "review_triggered", "prUrl", prUrl));
        } catch (Exception e) {
            log.error("Webhook processing failed: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }
}
