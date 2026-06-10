package com.pullcat.service.impl;

import com.pullcat.common.convention.exception.ClientException;
import com.pullcat.common.enums.CommonErrorCodeEnum;
import com.pullcat.common.enums.SessionStatus;
import com.pullcat.dto.resp.AnalysisResultRespDTO;
import com.pullcat.dto.resp.CreateReviewRespDTO;
import com.pullcat.dto.resp.PublishReviewRespDTO;
import com.pullcat.dto.resp.ReviewListRespDTO;
import com.pullcat.dto.resp.ReviewSessionRespDTO;
import com.pullcat.dto.resp.SseAnalysisStartedRespDTO;
import com.pullcat.dto.resp.SseCompletionRespDTO;
import com.pullcat.dto.resp.SseConnectedRespDTO;
import com.pullcat.dto.resp.SseMessageRespDTO;
import com.pullcat.dto.resp.SsePrInfoRespDTO;
import com.pullcat.config.infra.GitHubConfig;
import com.pullcat.remote.GitHubApiService;
import com.pullcat.service.ReviewService;
import com.pullcat.service.analysis.AnalysisOrchestrator;
import com.pullcat.service.analysis.ReviewPublisher;
import com.pullcat.service.analysis.ReviewSessionService;
import com.pullcat.service.analysis.StreamContext;
import com.pullcat.service.analysis.StreamRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Arrays;
import java.util.Map;
import java.util.UUID;

/**
 * 审查业务服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReviewServiceImpl implements ReviewService {

    private final GitHubApiService gitHubApiService;
    private final ReviewSessionService reviewSessionService;
    private final ReviewPublisher reviewPublisher;
    private final AnalysisOrchestrator analysisOrchestrator;
    private final GitHubConfig gitHubConfig;

    @Override
    public CreateReviewRespDTO createReview(String prUrl, String login) {
        if (prUrl == null || prUrl.isBlank()) {
            throw new ClientException(CommonErrorCodeEnum.CLIENT_ERROR.code(), "prUrl 不能为空");
        }

        ReviewSessionRespDTO session = createSession(prUrl, login);
        reviewSessionService.save(session);

        CreateReviewRespDTO response = new CreateReviewRespDTO();
        response.setReviewId(session.getId());
        response.setStatus(session.getStatus().name());
        response.setSseUrl("/api/pullcat/v1/reviews/" + session.getId() + "/sse");
        return response;
    }

    @Override
    public void triggerReview(String prUrl, Long installationId, String headSha) {
        ReviewSessionRespDTO session = createSession(prUrl, null);
        session.setInstallationId(installationId);
        session.setHeadSha(headSha);
        reviewSessionService.save(session);

        if (headSha != null && !headSha.isBlank()) {
            GitHubApiService apiService = resolveApiService(installationId);
            GitHubApiService.PRUrl parsed = gitHubApiService.parsePrUrl(prUrl);
            try {
                apiService.updateCommitStatus(parsed, headSha, "pending", "pullcat 审查中\u2026", null).block();
            } catch (Exception e) {
                log.warn("Failed to post pending commit status for {}: {}", prUrl, e.getMessage());
            }
        }

        analysisOrchestrator.startAsync(session);
    }

    private GitHubApiService resolveApiService(Long installationId) {
        if (installationId == null) {
            return gitHubApiService;
        }
        return gitHubApiService.withInstallationToken(installationId).blockOptional()
                .orElse(gitHubApiService);
    }

    @Override
    public ReviewListRespDTO listReviews(int page, int size, String repo, String login) {
        java.util.List<ReviewSessionRespDTO> reviews;
        long total;

        if (repo != null && !repo.isBlank()) {
            reviews = reviewSessionService.findByRepo(repo, page, size);
            total = reviewSessionService.countByRepo(repo);
        } else if (login != null) {
            reviews = reviewSessionService.findByLogin(login, page, size);
            total = reviewSessionService.countByLogin(login);
        } else {
            reviews = reviewSessionService.findAnonymous(page, size);
            total = reviewSessionService.countAnonymous();
        }

        ReviewListRespDTO response = new ReviewListRespDTO();
        response.setItems(reviews);
        response.setTotal(total);
        response.setPage(page);
        response.setSize(size);
        return response;
    }

    @Override
    public ReviewSessionRespDTO getReview(String id, String login) {
        ReviewSessionRespDTO session = reviewSessionService.findById(id);
        if (session == null) {
            throw new ClientException(CommonErrorCodeEnum.NOT_FOUND.code(), "审查记录不存在");
        }
        if (!isOwner(session, login)) {
            throw new ClientException(CommonErrorCodeEnum.FORBIDDEN.code(), "无权访问此审查");
        }
        return session;
    }

    @Override
    public void deleteReview(String id, String login) {
        ReviewSessionRespDTO session = reviewSessionService.findById(id);
        if (session == null) {
            throw new ClientException(CommonErrorCodeEnum.NOT_FOUND.code(), "审查记录不存在");
        }
        if (!isOwner(session, login)) {
            throw new ClientException(CommonErrorCodeEnum.FORBIDDEN.code(), "无权删除此审查");
        }
        reviewSessionService.delete(id);
    }

    @Override
    public PublishReviewRespDTO publishReview(String id, String login) {
        ReviewSessionRespDTO session = reviewSessionService.findById(id);
        if (session == null) {
            throw new ClientException(CommonErrorCodeEnum.NOT_FOUND.code(), "审查记录不存在");
        }
        if (!isOwner(session, login)) {
            throw new ClientException(CommonErrorCodeEnum.FORBIDDEN.code(), "无权发布此审查");
        }
        ReviewSessionRespDTO updated = reviewPublisher.publishReview(id);
        PublishReviewRespDTO response = new PublishReviewRespDTO();
        response.setStatus(updated.getStatus().name());
        response.setCommentId(updated.getPublishedCommentId());
        response.setPrUrl(updated.getPrUrl());
        return response;
    }

    @Override
    public SseEmitter startSseStream(String id, String login) {
        SseEmitter emitter = new SseEmitter(10 * 60 * 1000L);

        ReviewSessionRespDTO session = reviewSessionService.findById(id);
        if (session == null) {
            sendErrorAndComplete(emitter, "Review session not found");
            return emitter;
        }
        if (!isOwner(session, login)) {
            sendErrorAndComplete(emitter, "无权访问此审查");
            return emitter;
        }

        StreamContext ctx = new StreamContext(id, emitter);
        StreamRegistry.register(id, ctx);

        try {
            emitter.send(SseEmitter.event()
                    .name("connected")
                    .data(new SseConnectedRespDTO(id)));

            if (session.getStatus() == SessionStatus.FAILED) {
                emitter.send(SseEmitter.event().name("review_error")
                        .data(new SseMessageRespDTO("Review previously failed. Please start a new review.")));
                emitter.complete();
                return emitter;
            }

            if (session.getStatus() == SessionStatus.COMPLETED) {
                if (session.getPrMetadata() != null) {
                    SsePrInfoRespDTO prInfo = new SsePrInfoRespDTO();
                    prInfo.setPrUrl(session.getPrUrl());
                    prInfo.setMetadata(session.getPrMetadata());
                    prInfo.setDiff(session.getRawDiff() != null ? session.getRawDiff() : "");
                    emitter.send(SseEmitter.event().name("pr_info").data(prInfo));
                }
                for (Map.Entry<String, AnalysisResultRespDTO> entry : session.getAnalyses().entrySet()) {
                    emitter.send(SseEmitter.event().name("task_result").data(entry.getValue()));
                }
                emitter.send(SseEmitter.event().name("all_complete").data(new SseCompletionRespDTO("completed")));
                emitter.complete();
                return emitter;
            }

            emitter.send(SseEmitter.event()
                    .name("analysis_started")
                    .data(new SseAnalysisStartedRespDTO(Arrays.asList(
                            "summary", "risk", "quality", "consistency", "testing"))));

            if (session.getStatus() == SessionStatus.FETCHING) {
                analysisOrchestrator.startAsync(session);
            }

        } catch (IOException e) {
            emitter.completeWithError(e);
            StreamRegistry.remove(id);
        }

        emitter.onCompletion(() -> StreamRegistry.remove(id));
        emitter.onTimeout(() -> StreamRegistry.remove(id));
        emitter.onError(e -> StreamRegistry.remove(id));

        return emitter;
    }

    /**
     * 创建审查会话
     */
    private ReviewSessionRespDTO createSession(String prUrl, String userId) {
        GitHubApiService.PRUrl parsed = gitHubApiService.parsePrUrl(prUrl);

        ReviewSessionRespDTO session = new ReviewSessionRespDTO();
        session.setId(UUID.randomUUID().toString());
        session.setPrUrl(prUrl);
        session.setRepositoryFullName(parsed.owner() + "/" + parsed.repo());
        session.setStatus(SessionStatus.FETCHING);
        session.setUserId(userId);
        return session;
    }

    private void sendErrorAndComplete(SseEmitter emitter, String message) {
        try {
            emitter.send(SseEmitter.event().name("error").data(new SseMessageRespDTO(message)));
            emitter.complete();
        } catch (IOException e) {
            emitter.completeWithError(e);
        }
    }

    private boolean isOwner(ReviewSessionRespDTO session, String login) {
        if (session.getUserId() == null) {
            return login == null;
        }
        return session.getUserId().equals(login);
    }
}
