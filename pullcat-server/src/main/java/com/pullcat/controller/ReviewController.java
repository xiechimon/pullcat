package com.pullcat.controller;

import com.pullcat.model.ReviewSession;
import com.pullcat.service.analysis.AnalysisOrchestrator;
import com.pullcat.service.analysis.ReviewRepository;
import com.pullcat.service.analysis.StreamContext;
import com.pullcat.service.analysis.StreamRegistry;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Arrays;
import java.util.Map;

/**
 * 审查控制器，提供 PR 审查的创建、查看、SSE 事件流分发及结果发布等 REST 接口。
 */
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

    /**
     * 创建审查会话，对指定 PR 执行完整的五维度分析。
     *
     * @param body 请求体，需包含 "prUrl" 字段
     * @return 包含 reviewId、status 及 sseUrl 的响应
     */
    @PostMapping
    public ResponseEntity<Map<String, Object>> createReview(@RequestBody Map<String, String> body) {
        String prUrl = body.get("prUrl");
        if (prUrl == null || prUrl.isBlank()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "prUrl is required"));
        }

        ReviewSession session = orchestrator.createSession(prUrl);
        reviewRepository.save(session);

        // DO NOT start async review here. Wait for SSE to connect!

        return ResponseEntity.ok(Map.of(
                "reviewId", session.getId(),
                "status", session.getStatus().name(),
                "sseUrl", "/api/reviews/" + session.getId() + "/sse"
        ));
    }

    /**
     * 根据 ID 查询审查会话详情。
     *
     * @param id 审查会话唯一标识
     * @return 审查会话对象，不存在时返回 404
     */
    @GetMapping("/{id}")
    public ResponseEntity<ReviewSession> getReview(@PathVariable String id) {
        ReviewSession session = reviewRepository.findById(id);
        if (session == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(session);
    }

    /**
     * 建立 SSE 长连接，透传当前审查会话的分析事件流。
     *
     * @param id 审查会话唯一标识
     * @return SseEmitter 实例，客户端可通过 EventSource 消费事件
     */
    @GetMapping(value = "/{id}/sse", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamEvents(@PathVariable String id) {
        SseEmitter emitter = new SseEmitter(10 * 60 * 1000L);

        ReviewSession session = reviewRepository.findById(id);
        if (session == null) {
            try {
                emitter.send(SseEmitter.event()
                        .name("error")
                        .data(Map.of("message", "Review session not found")));
                emitter.complete();
            } catch (IOException e) {
                emitter.completeWithError(e);
            }
            return emitter;
        }

        StreamContext ctx = new StreamContext(id, emitter);
        StreamRegistry.register(id, ctx);

        try {
            emitter.send(SseEmitter.event()
                    .name("connected")
                    .data(Map.of("reviewId", id)));

            if (session.getStatus() == com.pullcat.model.SessionStatus.FAILED) {
                emitter.send(SseEmitter.event().name("review_error").data(Map.of("message", "Review previously failed. Please start a new review.")));
                emitter.complete();
                return emitter;
            }

            if (session.getStatus() == com.pullcat.model.SessionStatus.COMPLETED) {
                if (session.getPrMetadata() != null) {
                    emitter.send(SseEmitter.event().name("pr_info").data(Map.of("prUrl", session.getPrUrl(), "metadata", session.getPrMetadata())));
                }
                for (Map.Entry<String, com.pullcat.model.AnalysisResult> entry : session.getAnalyses().entrySet()) {
                    emitter.send(SseEmitter.event().name("task_result").data(entry.getValue()));
                }
                emitter.send(SseEmitter.event().name("all_complete").data(Map.of("status", "completed")));
                emitter.complete();
                return emitter;
            }

            emitter.send(SseEmitter.event()
                    .name("analysis_started")
                    .data(Map.of("tasks", Arrays.asList(
                            "summary", "risk", "quality", "consistency", "testing"))));

            if (session.getStatus() == com.pullcat.model.SessionStatus.FETCHING) {
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

    /**
     * 将审查结果发布为 PR 评论。
     *
     * @param id 审查会话唯一标识
     * @return 包含发布状态、评论 ID 及 PR URL 的响应，失败时返回 500
     */
    @PostMapping("/{id}/publish")
    public ResponseEntity<Map<String, Object>> publishReview(@PathVariable String id) {
        try {
            ReviewSession session = orchestrator.publishReview(id);
            return ResponseEntity.ok(Map.of(
                    "status", session.getStatus().name(),
                    "commentId", session.getPublishedCommentId(),
                    "prUrl", session.getPrUrl()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", e.getMessage()));
        }
    }
}
