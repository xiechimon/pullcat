package com.pullcat.controller;

import com.pullcat.common.convention.result.Result;
import com.pullcat.common.convention.result.Results;
import com.pullcat.dto.req.WebhookEventReqDTO;
import com.pullcat.dto.resp.WebhookRespDTO;
import com.pullcat.service.WebhookService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * Webhook 控制层
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/webhooks")
public class WebhookController {

    private final WebhookService webhookService;

    /**
     * 处理 GitHub Webhook 事件
     */
    @PostMapping("/github")
    public Result<WebhookRespDTO> handleGitHubWebhook(
            @RequestHeader("X-GitHub-Event") String eventType,
            @RequestBody WebhookEventReqDTO requestParam) {
        log.info("Received webhook event: {}", eventType);
        return Results.success(webhookService.handle(eventType, requestParam));
    }
}
