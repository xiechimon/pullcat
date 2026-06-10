package com.pullcat.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pullcat.common.convention.result.Result;
import com.pullcat.common.convention.result.Results;
import com.pullcat.config.infra.GitHubConfig;
import com.pullcat.dto.req.WebhookEventReqDTO;
import com.pullcat.dto.resp.WebhookRespDTO;
import com.pullcat.service.WebhookService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import org.springframework.http.HttpStatus;

/**
 * Webhook 控制层
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/pullcat/v1/webhooks")
public class WebhookController {

    private final WebhookService webhookService;
    private final GitHubConfig gitHubConfig;
    private final ObjectMapper objectMapper;

    /**
     * 处理 GitHub Webhook 事件
     */
    @PostMapping("/github")
    public Result<WebhookRespDTO> handleGitHubWebhook(
            @RequestHeader("X-GitHub-Event") String eventType,
            @RequestHeader("X-Hub-Signature-256") String signature,
            @RequestBody byte[] body) throws Exception {
        verifySignature(signature, body);
        WebhookEventReqDTO requestParam = objectMapper.readValue(body, WebhookEventReqDTO.class);
        log.info("Received webhook event: {}", eventType);
        return Results.success(webhookService.handle(eventType, requestParam));
    }

    private void verifySignature(String signature, byte[] body) throws Exception {
        String secret = gitHubConfig.getWebhookSecret();
        if (secret == null || secret.isBlank()) {
            return;
        }
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        byte[] expected = mac.doFinal(body);
        String actualHex = signature != null && signature.startsWith("sha256=")
                ? signature.substring("sha256=".length()) : "";
        byte[] actual = hexToBytes(actualHex);
        if (!MessageDigest.isEqual(expected, actual)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "GitHub webhook 签名校验失败");
        }
    }

    private byte[] hexToBytes(String hex) {
        if (hex.length() % 2 != 0) {
            return new byte[0];
        }
        byte[] bytes = new byte[hex.length() / 2];
        for (int i = 0; i < hex.length(); i += 2) {
            int value = Character.digit(hex.charAt(i), 16) << 4;
            value |= Character.digit(hex.charAt(i + 1), 16);
            if (value < 0) {
                return new byte[0];
            }
            bytes[i / 2] = (byte) value;
        }
        return bytes;
    }
}
