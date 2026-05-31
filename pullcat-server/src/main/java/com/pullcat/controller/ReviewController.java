package com.pullcat.controller;

import com.pullcat.model.Issue;
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

    @PostMapping
    public ResponseEntity<Map<String, Object>> createReview(@RequestBody Map<String, String> body) {
        String prUrl = body.get("prUrl");
        if (prUrl == null || prUrl.isBlank()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "prUrl is required"));
        }

        ReviewSession session = orchestrator.createSession(prUrl);
        reviewRepository.save(session);

        return ResponseEntity.ok(Map.of(
                "reviewId", session.getId(),
                "status", session.getStatus().name(),
                "sseUrl", "/api/reviews/" + session.getId() + "/sse"
        ));
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> listReviews(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String repo) {

        List<ReviewSession> reviews;
        long total;

        if (repo != null && !repo.isBlank()) {
            reviews = reviewRepository.findByRepo(repo, page, size);
            total = reviewRepository.countByRepo(repo);
        } else {
            reviews = reviewRepository.findAll(page, size);
            total = reviewRepository.count();
        }

        return ResponseEntity.ok(Map.of(
                "items", reviews,
                "total", total,
                "page", page,
                "size", size));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ReviewSession> getReview(@PathVariable String id) {
        ReviewSession session = reviewRepository.findById(id);
        if (session == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(session);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> deleteReview(@PathVariable String id) {
        if (!reviewRepository.exists(id)) {
            return ResponseEntity.notFound().build();
        }
        reviewRepository.delete(id);
        return ResponseEntity.ok(Map.of("deleted", true));
    }

    @PostMapping("/{reviewId}/issues/{issueId}/feedback")
    public ResponseEntity<Map<String, Object>> submitFeedback(
            @PathVariable String reviewId,
            @PathVariable String issueId,
            @RequestBody Map<String, Object> body) {

        ReviewSession session = reviewRepository.findById(reviewId);
        if (session == null) {
            return ResponseEntity.notFound().build();
        }

        boolean accepted = Boolean.TRUE.equals(body.get("accepted"));
        String reason = body.get("reason") != null ? body.get("reason").toString() : null;

        for (var entry : session.getAnalyses().entrySet()) {
            if (entry.getValue().getIssues() != null) {
                for (Issue issue : entry.getValue().getIssues()) {
                    if (issue.getId() != null && issue.getId().equals(issueId)) {
                        issue.setFeedback(accepted ? "ACCEPTED" : "REJECTED");
                        issue.setFeedbackReason(reason);
                        reviewRepository.save(session);
                        return ResponseEntity.ok(Map.of("status", "ok"));
                    }
                }
            }
        }

        return ResponseEntity.notFound().build();
    }

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

    @PostMapping("/{id}/publish")
    public ResponseEntity<Map<String, Object>> publishReview(@PathVariable String id) {
        ReviewSession session = orchestrator.publishReview(id);
        return ResponseEntity.ok(Map.of(
                "status", session.getStatus().name(),
                "commentId", session.getPublishedCommentId(),
                "prUrl", session.getPrUrl()));
    }
}
