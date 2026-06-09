package com.pullcat.controller;

import com.pullcat.common.convention.exception.ServiceException;
import com.pullcat.common.convention.result.Result;
import com.pullcat.common.convention.result.Results;
import com.pullcat.common.enums.CommonErrorCodeEnum;
import com.pullcat.dto.req.WebhookEventReqDTO;
import com.pullcat.dto.resp.WebhookRespDTO;
import com.pullcat.service.analysis.WebhookService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/webhooks")
public class WebhookController {

    private static final Logger log = LoggerFactory.getLogger(WebhookController.class);

    private final WebhookService webhookService;

    public WebhookController(WebhookService webhookService) {
        this.webhookService = webhookService;
    }

    @PostMapping("/github")
    public Result<WebhookRespDTO> handleGitHubWebhook(
            @RequestHeader("X-GitHub-Event") String eventType,
            @RequestBody WebhookEventReqDTO requestParam) {

        log.info("Received webhook event: {}", eventType);

        if (!"pull_request".equals(eventType)) {
            WebhookRespDTO response = new WebhookRespDTO();
            response.setStatus("ignored");
            response.setReason("not a PR event");
            return Results.success(response);
        }

        String action = requestParam.getAction();
        if (!"opened".equals(action) && !"synchronize".equals(action)) {
            WebhookRespDTO response = new WebhookRespDTO();
            response.setStatus("ignored");
            response.setAction(action);
            return Results.success(response);
        }

        try {
            String prUrl = requestParam.getPullRequest().getHtmlUrl();
            webhookService.triggerReview(prUrl);
            WebhookRespDTO response = new WebhookRespDTO();
            response.setStatus("review_triggered");
            response.setPrUrl(prUrl);
            return Results.success(response);
        } catch (Exception e) {
            log.error("Webhook processing failed: {}", e.getMessage(), e);
            throw new ServiceException(CommonErrorCodeEnum.SERVICE_ERROR.code(), e.getMessage() != null ? e.getMessage() : "Webhook 处理失败");
        }
    }
}
