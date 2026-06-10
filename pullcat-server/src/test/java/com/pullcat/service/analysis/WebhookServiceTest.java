package com.pullcat.service.analysis;

import com.pullcat.dto.req.WebhookEventReqDTO;
import com.pullcat.dto.req.WebhookEventReqDTO.InstallationReqDTO;
import com.pullcat.dto.req.WebhookPullRequestReqDTO;
import com.pullcat.dto.resp.WebhookRespDTO;
import com.pullcat.service.ReviewService;
import com.pullcat.service.analysis.GitHubInstallationService;
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
    ReviewService reviewService;

    @Mock
    GitHubInstallationService gitHubInstallationService;

    @InjectMocks
    WebhookServiceImpl webhookService;

    @Test
    void handle_nonPrEvent_returnsIgnored() {
        WebhookRespDTO result = webhookService.handle("push", new WebhookEventReqDTO());
        assertEquals("ignored", result.getStatus());
        assertEquals("not a PR event", result.getReason());
        verifyNoInteractions(reviewService);
    }

    @Test
    void handle_prEventUnsupportedAction_returnsIgnored() {
        WebhookEventReqDTO req = new WebhookEventReqDTO();
        req.setAction("closed");
        WebhookRespDTO result = webhookService.handle("pull_request", req);
        assertEquals("ignored", result.getStatus());
        assertEquals("closed", result.getAction());
        verifyNoInteractions(reviewService);
    }

    @Test
    void handle_prOpened_triggersReview() {
        WebhookPullRequestReqDTO pr = new WebhookPullRequestReqDTO();
        pr.setHtmlUrl("https://github.com/a/b/pull/1");
        WebhookEventReqDTO req = new WebhookEventReqDTO();
        req.setAction("opened");
        req.setPullRequest(pr);
        InstallationReqDTO installation = new InstallationReqDTO();
        installation.setId(1001L);
        req.setInstallation(installation);

        WebhookRespDTO result = webhookService.handle("pull_request", req);

        assertEquals("review_triggered", result.getStatus());
        assertEquals("https://github.com/a/b/pull/1", result.getPrUrl());
        verify(reviewService).triggerReview("https://github.com/a/b/pull/1", 1001L);
    }

    @Test
    void handle_prSynchronize_triggersReview() {
        WebhookPullRequestReqDTO pr = new WebhookPullRequestReqDTO();
        pr.setHtmlUrl("https://github.com/a/b/pull/2");
        WebhookEventReqDTO req = new WebhookEventReqDTO();
        req.setAction("synchronize");
        req.setPullRequest(pr);
        InstallationReqDTO installation = new InstallationReqDTO();
        installation.setId(1002L);
        req.setInstallation(installation);

        WebhookRespDTO result = webhookService.handle("pull_request", req);

        assertEquals("review_triggered", result.getStatus());
        verify(reviewService).triggerReview("https://github.com/a/b/pull/2", 1002L);
    }

    @Test
    void handle_installationCreated_savesInstallation() {
        WebhookEventReqDTO req = new WebhookEventReqDTO();
        req.setAction("created");
        InstallationReqDTO installation = new InstallationReqDTO();
        installation.setId(3001L);
        installation.setAccount(new InstallationReqDTO.AccountReqDTO());
        installation.getAccount().setLogin("octo-org");
        installation.getAccount().setType("Organization");
        req.setInstallation(installation);

        WebhookRespDTO result = webhookService.handle("installation", req);

        assertEquals("installation_processed", result.getStatus());
        verify(gitHubInstallationService).saveInstallation(3001L, "octo-org", "Organization");
        verifyNoInteractions(reviewService);
    }

    @Test
    void handle_installationDeleted_suspendsInstallation() {
        WebhookEventReqDTO req = new WebhookEventReqDTO();
        req.setAction("deleted");
        InstallationReqDTO installation = new InstallationReqDTO();
        installation.setId(3002L);
        req.setInstallation(installation);

        WebhookRespDTO result = webhookService.handle("installation", req);

        assertEquals("installation_processed", result.getStatus());
        verify(gitHubInstallationService).suspendInstallation(3002L);
        verifyNoInteractions(reviewService);
    }
}
