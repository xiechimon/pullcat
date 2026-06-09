package com.pullcat.service;

import com.pullcat.common.convention.exception.ClientException;
import com.pullcat.common.enums.CommonErrorCodeEnum;
import com.pullcat.dto.req.PublishReqDTO;
import com.pullcat.dto.resp.*;
import com.pullcat.service.analysis.AnalysisOrchestrator;
import com.pullcat.service.analysis.ReviewRepository;
import com.pullcat.service.analysis.StreamContext;
import com.pullcat.service.analysis.StreamRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * 审查业务服务
 */
@Service
@RequiredArgsConstructor
public class ReviewService {

    private final AnalysisOrchestrator orchestrator;
    private final ReviewRepository reviewRepository;

    /**
     * 分页查询审查列表
     */
    public ReviewListRespDTO listReviews(int page, int size, String repo, String login) {
        List<ReviewSessionRespDTO> reviews;
        long total;

        if (repo != null && !repo.isBlank()) {
            reviews = reviewRepository.findByRepo(repo, page, size);
            total = reviewRepository.countByRepo(repo);
        } else if (login != null) {
            reviews = reviewRepository.findByLogin(login, page, size);
            total = reviewRepository.countByLogin(login);
        } else {
            reviews = reviewRepository.findAnonymous(page, size);
            total = reviewRepository.countAnonymous();
        }

        ReviewListRespDTO response = new ReviewListRespDTO();
        response.setItems(reviews);
        response.setTotal(total);
        response.setPage(page);
        response.setSize(size);
        return response;
    }

    /**
     * 获取单条审查详情
     */
    public ReviewSessionRespDTO getReview(String id, String login) {
        ReviewSessionRespDTO session = reviewRepository.findById(id);
        if (session == null) {
            throw new ClientException(CommonErrorCodeEnum.NOT_FOUND.code(), "审查记录不存在");
        }
        if (!isOwner(session, login)) {
            throw new ClientException(CommonErrorCodeEnum.FORBIDDEN.code(), "无权访问此审查");
        }
        return session;
    }

    /**
     * 删除审查记录
     */
    public void deleteReview(String id, String login) {
        ReviewSessionRespDTO session = reviewRepository.findById(id);
        if (session == null) {
            throw new ClientException(CommonErrorCodeEnum.NOT_FOUND.code(), "审查记录不存在");
        }
        if (!isOwner(session, login)) {
            throw new ClientException(CommonErrorCodeEnum.FORBIDDEN.code(), "无权删除此审查");
        }
        reviewRepository.delete(id);
    }

    /**
     * 创建审查会话并返回初始信息
     */
    public CreateReviewRespDTO createReview(String prUrl, String login) {
        if (prUrl == null || prUrl.isBlank()) {
            throw new ClientException(CommonErrorCodeEnum.CLIENT_ERROR.code(), "prUrl 不能为空");
        }
        ReviewSessionRespDTO session = orchestrator.createSession(prUrl, login);
        reviewRepository.save(session);

        CreateReviewRespDTO response = new CreateReviewRespDTO();
        response.setReviewId(session.getId());
        response.setStatus(session.getStatus().name());
        response.setSseUrl("/api/reviews/" + session.getId() + "/sse");
        return response;
    }

    /**
     * 提交问题反馈（接受或拒绝）
     */
    public StatusRespDTO submitFeedback(String reviewId, String issueId, boolean accepted, String reason, String login) {
        ReviewSessionRespDTO session = reviewRepository.findById(reviewId);
        if (session == null) {
            throw new ClientException(CommonErrorCodeEnum.NOT_FOUND.code(), "审查记录不存在");
        }
        if (!isOwner(session, login)) {
            throw new ClientException(CommonErrorCodeEnum.FORBIDDEN.code(), "无权操作此审查");
        }

        for (var entry : session.getAnalyses().entrySet()) {
            if (entry.getValue().getIssues() != null) {
                for (IssueRespDTO issue : entry.getValue().getIssues()) {
                    if (issue.getId() != null && issue.getId().equals(issueId)) {
                        issue.setFeedback(accepted ? "ACCEPTED" : "REJECTED");
                        issue.setFeedbackReason(reason);
                        reviewRepository.save(session);
                        return new StatusRespDTO("ok");
                    }
                }
            }
        }
        throw new ClientException(CommonErrorCodeEnum.NOT_FOUND.code(), "问题不存在");
    }

    /**
     * 将审查结果发布到 PR 评论
     */
    public PublishReviewRespDTO publishReview(String id, PublishReqDTO requestParam, String login) {
        ReviewSessionRespDTO session = reviewRepository.findById(id);
        if (session == null) {
            throw new ClientException(CommonErrorCodeEnum.NOT_FOUND.code(), "审查记录不存在");
        }
        if (!isOwner(session, login)) {
            throw new ClientException(CommonErrorCodeEnum.FORBIDDEN.code(), "无权发布此审查");
        }
        ReviewSessionRespDTO updated = orchestrator.publishReview(id, requestParam);
        PublishReviewRespDTO response = new PublishReviewRespDTO();
        response.setStatus(updated.getStatus().name());
        response.setCommentId(updated.getPublishedCommentId());
        response.setPrUrl(updated.getPrUrl());
        return response;
    }

    /**
     * SSE 流式推送分析进度与结果
     */
    public SseEmitter startSseStream(String id, String login) {
        SseEmitter emitter = new SseEmitter(10 * 60 * 1000L);

        ReviewSessionRespDTO session = reviewRepository.findById(id);
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

            if (session.getStatus() == com.pullcat.common.enums.SessionStatus.FAILED) {
                emitter.send(SseEmitter.event().name("review_error")
                        .data(new SseMessageRespDTO("Review previously failed. Please start a new review.")));
                emitter.complete();
                return emitter;
            }

            if (session.getStatus() == com.pullcat.common.enums.SessionStatus.COMPLETED) {
                if (session.getPrMetadata() != null) {
                    SsePrInfoRespDTO prInfo = new SsePrInfoRespDTO();
                    prInfo.setPrUrl(session.getPrUrl());
                    prInfo.setMetadata(session.getPrMetadata());
                    prInfo.setDiff(session.getRawDiff() != null ? session.getRawDiff() : "");
                    emitter.send(SseEmitter.event().name("pr_info").data(prInfo));
                }
                for (Map.Entry<String, com.pullcat.dto.resp.AnalysisResultRespDTO> entry
                        : session.getAnalyses().entrySet()) {
                    emitter.send(SseEmitter.event().name("task_result").data(entry.getValue()));
                }
                emitter.send(SseEmitter.event().name("all_complete")
                        .data(new SseCompletionRespDTO("completed")));
                emitter.complete();
                return emitter;
            }

            emitter.send(SseEmitter.event()
                    .name("analysis_started")
                    .data(new SseAnalysisStartedRespDTO(Arrays.asList(
                            "summary", "risk", "quality", "consistency", "testing"))));

            if (session.getStatus() == com.pullcat.common.enums.SessionStatus.FETCHING) {
                orchestrator.startReviewAsync(session);
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

    private void sendErrorAndComplete(SseEmitter emitter, String message) {
        try {
            emitter.send(SseEmitter.event().name("error").data(new SseMessageRespDTO(message)));
            emitter.complete();
        } catch (IOException e) {
            emitter.completeWithError(e);
        }
    }

    /**
     * 校验当前用户是否为审查所有者
     */
    private boolean isOwner(ReviewSessionRespDTO session, String login) {
        if (session.getUserId() == null) {
            return login == null;
        }
        return session.getUserId().equals(login);
    }
}
