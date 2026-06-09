package com.pullcat.controller;

import com.pullcat.common.convention.result.Result;
import com.pullcat.common.convention.result.Results;
import com.pullcat.dto.req.CreateReviewReqDTO;
import com.pullcat.dto.req.IssueFeedbackReqDTO;
import com.pullcat.dto.req.PublishReqDTO;
import com.pullcat.dto.resp.*;
import com.pullcat.service.ReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * 审查控制层
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/reviews")
public class ReviewController {

    private final ReviewService reviewService;

    /**
     * 从认证信息中提取 GitHub 用户名
     */
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

    /**
     * 创建审查会话并返回 SSE 地址
     */
    @PostMapping
    public Result<CreateReviewRespDTO> createReview(
            @RequestBody CreateReviewReqDTO requestParam,
            @AuthenticationPrincipal OAuth2User principal) {
        return Results.success(reviewService.createReview(requestParam.getPrUrl(), extractLogin(principal)));
    }

    /**
     * 分页查询审查列表
     */
    @GetMapping
    public Result<ReviewListRespDTO> listReviews(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String repo,
            @AuthenticationPrincipal OAuth2User principal) {
        return Results.success(reviewService.listReviews(page, size, repo, extractLogin(principal)));
    }

    /**
     * 获取单条审查详情
     */
    @GetMapping("/{id}")
    public Result<ReviewSessionRespDTO> getReview(
            @PathVariable String id,
            @AuthenticationPrincipal OAuth2User principal) {
        return Results.success(reviewService.getReview(id, extractLogin(principal)));
    }

    /**
     * 删除审查记录
     */
    @DeleteMapping("/{id}")
    public Result<DeletedRespDTO> deleteReview(
            @PathVariable String id,
            @AuthenticationPrincipal OAuth2User principal) {
        reviewService.deleteReview(id, extractLogin(principal));
        return Results.success(new DeletedRespDTO(true));
    }

    /**
     * 提交问题反馈（接受或拒绝）
     */
    @PostMapping("/{reviewId}/issues/{issueId}/feedback")
    public Result<StatusRespDTO> submitFeedback(
            @PathVariable String reviewId,
            @PathVariable String issueId,
            @RequestBody IssueFeedbackReqDTO requestParam,
            @AuthenticationPrincipal OAuth2User principal) {
        boolean accepted = Boolean.TRUE.equals(requestParam.getAccepted());
        return Results.success(reviewService.submitFeedback(reviewId, issueId, accepted,
                requestParam.getReason(), extractLogin(principal)));
    }

    /**
     * SSE 流式推送分析进度与结果
     */
    @GetMapping(value = "/{id}/sse", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamEvents(
            @PathVariable String id,
            @AuthenticationPrincipal OAuth2User principal) {
        return reviewService.startSseStream(id, extractLogin(principal));
    }

    /**
     * 将审查结果发布到 PR 评论
     */
    @PostMapping("/{id}/publish")
    public Result<PublishReviewRespDTO> publishReview(
            @PathVariable String id,
            @RequestBody PublishReqDTO requestParam,
            @AuthenticationPrincipal OAuth2User principal) {
        return Results.success(reviewService.publishReview(id, requestParam, extractLogin(principal)));
    }
}
