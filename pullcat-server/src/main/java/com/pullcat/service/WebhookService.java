package com.pullcat.service;

import com.pullcat.dto.req.WebhookEventReqDTO;
import com.pullcat.dto.resp.ReviewSessionRespDTO;
import com.pullcat.dto.resp.WebhookRespDTO;
import com.pullcat.service.analysis.AnalysisOrchestrator;
import com.pullcat.service.analysis.ReviewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Webhook 业务服务
 */
@Service
@RequiredArgsConstructor
public class WebhookService {

    private final AnalysisOrchestrator orchestrator;
    private final ReviewRepository reviewRepository;

    /**
     * 处理 GitHub Webhook 事件，返回处理结果
     */
    public WebhookRespDTO handle(String eventType, WebhookEventReqDTO requestParam) {
        WebhookRespDTO response = new WebhookRespDTO();

        if (!"pull_request".equals(eventType)) {
            response.setStatus("ignored");
            response.setReason("not a PR event");
            return response;
        }

        String action = requestParam.getAction();
        if (!"opened".equals(action) && !"synchronize".equals(action)) {
            response.setStatus("ignored");
            response.setAction(action);
            return response;
        }

        String prUrl = requestParam.getPullRequest().getHtmlUrl();
        triggerReview(prUrl);
        response.setStatus("review_triggered");
        response.setPrUrl(prUrl);
        return response;
    }

    private void triggerReview(String prUrl) {
        ReviewSessionRespDTO session = orchestrator.createSession(prUrl, null);
        reviewRepository.save(session);
        orchestrator.startReviewAsync(session);
    }
}
