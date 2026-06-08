package com.pullcat.controller;

import com.pullcat.common.convention.exception.ClientException;
import com.pullcat.common.convention.result.Result;
import com.pullcat.common.convention.result.Results;
import com.pullcat.common.enums.CommonErrorCodeEnum;
import com.pullcat.dto.req.CreateReviewReqDTO;
import com.pullcat.dto.req.IssueFeedbackReqDTO;
import com.pullcat.dto.req.PublishReqDTO;
import com.pullcat.dto.resp.CreateReviewRespDTO;
import com.pullcat.dto.resp.DeletedRespDTO;
import com.pullcat.dto.resp.IssueRespDTO;
import com.pullcat.dto.resp.PublishReviewRespDTO;
import com.pullcat.dto.resp.ReviewListRespDTO;
import com.pullcat.dto.resp.ReviewSessionRespDTO;
import com.pullcat.dto.resp.SseAnalysisStartedRespDTO;
import com.pullcat.dto.resp.SseCompletionRespDTO;
import com.pullcat.dto.resp.SseConnectedRespDTO;
import com.pullcat.dto.resp.SseMessageRespDTO;
import com.pullcat.dto.resp.SsePrInfoRespDTO;
import com.pullcat.dto.resp.StatusRespDTO;
import com.pullcat.service.analysis.AnalysisOrchestrator;
import com.pullcat.service.analysis.ReviewRepository;
import com.pullcat.service.analysis.StreamContext;
import com.pullcat.service.analysis.StreamRegistry;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/reviews")
public class ReviewController {

    private final AnalysisOrchestrator orchestrator;
    private final ReviewRepository reviewRepository;

    @Lazy
    public ReviewController(AnalysisOrchestrator orchestrator,
                            ReviewRepository reviewRepository) {
        this.orchestrator = orchestrator;
        this.reviewRepository = reviewRepository;
    }

    private static String extractLogin(OAuth2User principal) {
        if (principal != null) {
            return principal.getAttribute("login");
        }
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth instanceof OAuth2AuthenticationToken oauth) {
            return oauth.getName();
        }
        return null;
    }

    @PostMapping
    public ResponseEntity<Result<CreateReviewRespDTO>> createReview(@RequestBody CreateReviewReqDTO requestParam,
                                                                    @AuthenticationPrincipal OAuth2User principal) {
        String prUrl = requestParam.getPrUrl();
        if (prUrl == null || prUrl.isBlank()) {
            throw new ClientException(CommonErrorCodeEnum.CLIENT_ERROR.code(), "prUrl 不能为空");
        }

        String login = extractLogin(principal);
        ReviewSessionRespDTO session = orchestrator.createSession(prUrl, login);
        reviewRepository.save(session);

        CreateReviewRespDTO response = new CreateReviewRespDTO();
        response.setReviewId(session.getId());
        response.setStatus(session.getStatus().name());
        response.setSseUrl("/api/reviews/" + session.getId() + "/sse");
        return ResponseEntity.ok(Results.success(response));
    }

    @GetMapping
    public ResponseEntity<Result<ReviewListRespDTO>> listReviews(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String repo,
            @AuthenticationPrincipal OAuth2User principal) {

        String login = extractLogin(principal);
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
        return ResponseEntity.ok(Results.success(response));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Result<ReviewSessionRespDTO>> getReview(@PathVariable String id,
                                                           @AuthenticationPrincipal OAuth2User principal) {
        ReviewSessionRespDTO session = reviewRepository.findById(id);
        if (session == null) {
            throw new ClientException(CommonErrorCodeEnum.NOT_FOUND.code(), "审查记录不存在");
        }
        String login = extractLogin(principal);
        if (!isOwner(session, login)) {
            throw new ClientException(CommonErrorCodeEnum.FORBIDDEN.code(), "无权访问此审查");
        }
        return ResponseEntity.ok(Results.success(session));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Result<DeletedRespDTO>> deleteReview(@PathVariable String id,
                                                                    @AuthenticationPrincipal OAuth2User principal) {
        ReviewSessionRespDTO session = reviewRepository.findById(id);
        if (session == null) {
            throw new ClientException(CommonErrorCodeEnum.NOT_FOUND.code(), "审查记录不存在");
        }
        String login = extractLogin(principal);
        if (!isOwner(session, login)) {
            throw new ClientException(CommonErrorCodeEnum.FORBIDDEN.code(), "无权删除此审查");
        }
        reviewRepository.delete(id);
        return ResponseEntity.ok(Results.success(new DeletedRespDTO(true)));
    }

    @PostMapping("/{reviewId}/issues/{issueId}/feedback")
    public ResponseEntity<Result<StatusRespDTO>> submitFeedback(
            @PathVariable String reviewId,
            @PathVariable String issueId,
            @RequestBody IssueFeedbackReqDTO requestParam,
            @AuthenticationPrincipal OAuth2User principal) {

        ReviewSessionRespDTO session = reviewRepository.findById(reviewId);
        if (session == null) {
            throw new ClientException(CommonErrorCodeEnum.NOT_FOUND.code(), "审查记录不存在");
        }
        String login = extractLogin(principal);
        if (!isOwner(session, login)) {
            throw new ClientException(CommonErrorCodeEnum.FORBIDDEN.code(), "无权操作此审查");
        }

        boolean accepted = Boolean.TRUE.equals(requestParam.getAccepted());
        String reason = requestParam.getReason();

        for (var entry : session.getAnalyses().entrySet()) {
            if (entry.getValue().getIssues() != null) {
                for (IssueRespDTO issue : entry.getValue().getIssues()) {
                    if (issue.getId() != null && issue.getId().equals(issueId)) {
                        issue.setFeedback(accepted ? "ACCEPTED" : "REJECTED");
                        issue.setFeedbackReason(reason);
                        reviewRepository.save(session);
                        return ResponseEntity.ok(Results.success(new StatusRespDTO("ok")));
                    }
                }
            }
        }

        throw new ClientException(CommonErrorCodeEnum.NOT_FOUND.code(), "问题不存在");
    }

    @GetMapping(value = "/{id}/sse", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamEvents(@PathVariable String id,
                                   @AuthenticationPrincipal OAuth2User principal) {
        SseEmitter emitter = new SseEmitter(10 * 60 * 1000L);

        ReviewSessionRespDTO session = reviewRepository.findById(id);
        if (session == null) {
            sendErrorAndComplete(emitter, "Review session not found");
            return emitter;
        }
        String login = extractLogin(principal);
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
                for (Map.Entry<String, com.pullcat.dto.resp.AnalysisResultRespDTO> entry : session.getAnalyses().entrySet()) {
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

    @PostMapping("/{id}/publish")
    public ResponseEntity<Result<PublishReviewRespDTO>> publishReview(@PathVariable String id,
                                                                     @RequestBody PublishReqDTO requestParam,
                                                                     @AuthenticationPrincipal OAuth2User principal) {
        ReviewSessionRespDTO session = reviewRepository.findById(id);
        if (session == null) {
            throw new ClientException(CommonErrorCodeEnum.NOT_FOUND.code(), "审查记录不存在");
        }
        String login = extractLogin(principal);
        if (!isOwner(session, login)) {
            throw new ClientException(CommonErrorCodeEnum.FORBIDDEN.code(), "无权发布此审查");
        }
        ReviewSessionRespDTO updated = orchestrator.publishReview(id, requestParam);
        PublishReviewRespDTO response = new PublishReviewRespDTO();
        response.setStatus(updated.getStatus().name());
        response.setCommentId(updated.getPublishedCommentId());
        response.setPrUrl(updated.getPrUrl());
        return ResponseEntity.ok(Results.success(response));
    }

    private boolean isOwner(ReviewSessionRespDTO session, String login) {
        if (session.getUserId() == null) {
            return login == null;
        }
        return session.getUserId().equals(login);
    }

    private void sendErrorAndComplete(SseEmitter emitter, String message) {
        try {
            emitter.send(SseEmitter.event().name("error").data(new SseMessageRespDTO(message)));
            emitter.complete();
        } catch (IOException e) {
            emitter.completeWithError(e);
        }
    }
}
