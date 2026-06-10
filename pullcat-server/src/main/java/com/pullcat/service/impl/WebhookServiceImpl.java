package com.pullcat.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.pullcat.dao.entity.ReviewDO;
import com.pullcat.dao.mapper.ReviewMapper;
import com.pullcat.dto.req.WebhookEventReqDTO;
import com.pullcat.dto.req.WebhookPullRequestReqDTO;
import com.pullcat.dto.resp.WebhookRespDTO;
import com.pullcat.service.ReviewService;
import com.pullcat.service.WebhookService;
import com.pullcat.service.analysis.GitHubInstallationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class WebhookServiceImpl extends ServiceImpl<ReviewMapper, ReviewDO> implements WebhookService {

    private final ReviewService reviewService;
    private final GitHubInstallationService gitHubInstallationService;

    @Override
    public WebhookRespDTO handle(String eventType, WebhookEventReqDTO requestParam) {
        WebhookRespDTO response = new WebhookRespDTO();

        if ("installation".equals(eventType)) {
            handleInstallationEvent(requestParam);
            response.setStatus("installation_processed");
            response.setAction(requestParam.getAction());
            return response;
        }

        if (!"pull_request".equals(eventType)) {
            response.setStatus("ignored");
            response.setReason("not a PR event");
            return response;
        }

        String action = requestParam.getAction();
        if (!"opened".equals(action) && !"synchronize".equals(action) && !"reopened".equals(action)) {
            response.setStatus("ignored");
            response.setAction(action);
            return response;
        }

        WebhookPullRequestReqDTO pr = requestParam.getPullRequest();
        String prUrl = pr.getHtmlUrl();
        Long installationId = requestParam.getInstallation() != null ? requestParam.getInstallation().getId() : null;
        String headSha = pr.getHead() != null ? pr.getHead().getSha() : null;
        reviewService.triggerReview(prUrl, installationId, headSha);
        response.setStatus("review_triggered");
        response.setPrUrl(prUrl);
        return response;
    }

    private void handleInstallationEvent(WebhookEventReqDTO requestParam) {
        WebhookEventReqDTO.InstallationReqDTO installation = requestParam.getInstallation();
        if (installation == null || installation.getId() == null) {
            return;
        }
        String action = requestParam.getAction();
        if ("created".equals(action) || "unsuspend".equals(action) || "new_permissions_accepted".equals(action)) {
            String login = installation.getAccount() != null ? installation.getAccount().getLogin() : null;
            String type = installation.getAccount() != null ? installation.getAccount().getType() : null;
            gitHubInstallationService.saveInstallation(installation.getId(), login, type);
            return;
        }
        if ("deleted".equals(action) || "suspend".equals(action)) {
            gitHubInstallationService.suspendInstallation(installation.getId());
        }
    }
}
