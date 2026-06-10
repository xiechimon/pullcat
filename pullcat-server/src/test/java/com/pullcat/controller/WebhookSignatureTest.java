package com.pullcat.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pullcat.config.infra.GitHubConfig;
import com.pullcat.dto.resp.WebhookRespDTO;
import com.pullcat.service.WebhookService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(WebhookController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GitHubConfig.class)
class WebhookSignatureTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private GitHubConfig gitHubConfig;

    @MockBean
    private WebhookService webhookService;

    @Test
    void handleGitHubWebhook_rejectsInvalidSignature() throws Exception {
        gitHubConfig.setWebhookSecret("test-secret");
        String body = objectMapper.writeValueAsString(new TestWebhookPayload("opened", "https://github.com/a/b/pull/1"));

        mockMvc.perform(post("/api/pullcat/v1/webhooks/github")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-GitHub-Event", "pull_request")
                        .header("X-Hub-Signature-256", "sha256=invalid")
                        .content(body))
                .andExpect(status().isForbidden());
    }

    @Test
    void handleGitHubWebhook_acceptsValidSignature() throws Exception {
        gitHubConfig.setWebhookSecret("test-secret");
        String body = objectMapper.writeValueAsString(new TestWebhookPayload("opened", "https://github.com/a/b/pull/1"));
        WebhookRespDTO response = new WebhookRespDTO();
        response.setStatus("review_triggered");
        when(webhookService.handle(eq("pull_request"), any())).thenReturn(response);

        mockMvc.perform(post("/api/pullcat/v1/webhooks/github")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-GitHub-Event", "pull_request")
                        .header("X-Hub-Signature-256", signature("test-secret", body))
                        .content(body))
                .andExpect(status().isOk());

        verify(webhookService).handle(eq("pull_request"), any());
    }

    private String signature(String secret, String body) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        byte[] bytes = mac.doFinal(body.getBytes(StandardCharsets.UTF_8));
        StringBuilder builder = new StringBuilder("sha256=");
        for (byte b : bytes) {
            builder.append(String.format("%02x", b));
        }
        return builder.toString();
    }

    private record TestWebhookPayload(String action, PullRequest pull_request) {

        private TestWebhookPayload(String action, String htmlUrl) {
            this(action, new PullRequest(htmlUrl));
        }
    }

    private record PullRequest(String html_url) {
    }
}
