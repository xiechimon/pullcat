package com.pullcat.controller;

import com.pullcat.common.biz.user.CurrentLogin;
import com.pullcat.common.convention.result.Result;
import com.pullcat.common.convention.result.Results;
import com.pullcat.dto.req.CreateReviewReqDTO;
import com.pullcat.dto.req.PublishReqDTO;
import com.pullcat.dto.resp.*;
import com.pullcat.service.ReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * 审查控制层
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/pullcat/v1/reviews")
public class ReviewController {

    private final ReviewService reviewService;

    /**
     * 创建审查会话并返回 SSE 地址
     */
    @PostMapping
    public Result<CreateReviewRespDTO> createReview(
            @RequestBody CreateReviewReqDTO requestParam,
            @CurrentLogin String login) {
        return Results.success(reviewService.createReview(requestParam.getPrUrl(), login));
    }

    /**
     * 分页查询审查列表
     */
    @GetMapping
    public Result<ReviewListRespDTO> listReviews(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String repo,
            @CurrentLogin String login) {
        return Results.success(reviewService.listReviews(page, size, repo, login));
    }

    /**
     * 获取单条审查详情
     */
    @GetMapping("/{id}")
    public Result<ReviewSessionRespDTO> getReview(
            @PathVariable String id,
            @CurrentLogin String login) {
        return Results.success(reviewService.getReview(id, login));
    }

    /**
     * 删除审查记录
     */
    @DeleteMapping("/{id}")
    public Result<DeletedRespDTO> deleteReview(
            @PathVariable String id,
            @CurrentLogin String login) {
        reviewService.deleteReview(id, login);
        return Results.success(new DeletedRespDTO(true));
    }

    /**
     * SSE 流式推送分析进度与结果
     */
    @GetMapping(value = "/{id}/sse", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamEvents(
            @PathVariable String id,
            @CurrentLogin String login) {
        return reviewService.startSseStream(id, login);
    }

    /**
     * 将审查结果发布到 PR 评论
     */
    @PostMapping("/{id}/publish")
    public Result<PublishReviewRespDTO> publishReview(
            @PathVariable String id,
            @RequestBody PublishReqDTO requestParam,
            @CurrentLogin String login) {
        return Results.success(reviewService.publishReview(id, requestParam, login));
    }
}
