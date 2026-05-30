package com.pullcat.service.analysis;

import com.pullcat.model.*;
import com.pullcat.service.github.GitHubApiService;
import com.pullcat.service.llm.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * 分析编排器，负责完整的 PR 审查流程：拉取数据、构建上下文、执行五个分析任务，
 * 并通过 SSE 实时推送进度。
 */
@Slf4j
@Service
public class AnalysisOrchestrator {

    private final GitHubApiService gitHubApiService;
    private final PromptLoader promptLoader;
    private final ContextBuilder contextBuilder;
    private final ReviewRepository reviewRepository;
    private final ChatClient chatClient;

    public AnalysisOrchestrator(GitHubApiService gitHubApiService,
                                PromptLoader promptLoader,
                                ContextBuilder contextBuilder,
                                ReviewRepository reviewRepository,
                                ChatClient chatClient) {
        this.gitHubApiService = gitHubApiService;
        this.promptLoader = promptLoader;
        this.contextBuilder = contextBuilder;
        this.reviewRepository = reviewRepository;
        this.chatClient = chatClient;
    }

    /**
     * 创建审查会话，仅解析 URL 并保存初始状态到 Redis，不启动分析。
     */
    public ReviewSession createSession(String prUrl) {
        gitHubApiService.parsePrUrl(prUrl);

        ReviewSession session = new ReviewSession();
        session.setId(UUID.randomUUID().toString());
        session.setPrUrl(prUrl);
        session.setStatus(SessionStatus.FETCHING);
        return session;
    }

    /**
     * 异步执行完整的 PR 审查流程，通过 SSE 实时推送进度。
     */
    public void startReviewAsync(ReviewSession session) {
        new Thread(() -> {
            try {
                GitHubApiService.PRUrl parsed = gitHubApiService.parsePrUrl(session.getPrUrl());

                PRData prData = gitHubApiService.fetchPRData(parsed).block();
                session.setPrMetadata(prData.getMetadata());
                session.setStatus(SessionStatus.ANALYZING);
                reviewRepository.save(session);

                StreamContext metaCtx = StreamRegistry.get(session.getId());
                if (metaCtx != null) {
                    metaCtx.emitter().send(SseEmitter.event()
                            .name("pr_info")
                            .data(Map.of(
                                    "prUrl", session.getPrUrl(),
                                    "metadata", prData.getMetadata())));
                }

                Map<String, String> variables = contextBuilder.buildVariables(
                        prData.getMetadata(), prData.getFileTree(), prData.getFiles());

                session.getAnalyses().put("summary", executeTask(AnalysisType.SUMMARY, variables, session.getId()));
                session.getAnalyses().put("risk", executeTask(AnalysisType.RISK, variables, session.getId()));
                session.getAnalyses().put("quality", executeTask(AnalysisType.QUALITY, variables, session.getId()));
                session.getAnalyses().put("consistency", executeTask(AnalysisType.CONSISTENCY, variables, session.getId()));
                session.getAnalyses().put("testing", executeTask(AnalysisType.TESTING, variables, session.getId()));

                session.setStatus(SessionStatus.COMPLETED);
                reviewRepository.save(session);

                StreamContext finalCtx = StreamRegistry.get(session.getId());
                if (finalCtx != null) {
                    finalCtx.emitter().send(SseEmitter.event().name("all_complete").data(Map.of("status", "completed")));
                    finalCtx.emitter().complete();
                }
            } catch (Exception e) {
                log.error("Review failed: {}", e.getMessage(), e);
                session.setStatus(SessionStatus.FAILED);
                reviewRepository.save(session);
                StreamContext finalCtx = StreamRegistry.get(session.getId());
                if (finalCtx != null) {
                    try {
                        finalCtx.emitter().send(SseEmitter.event().name("review_error").data(Map.of("message", e.getMessage())));
                        finalCtx.emitter().complete();
                    } catch (IOException ignored) {}
                }
            }
        }, "review-" + session.getId()).start();
    }

    private AnalysisResult executeTask(AnalysisType type, Map<String, String> variables, String sessionId) {
        AnalysisTask task = createTask(type);
        String template = promptLoader.loadTemplate(type.getTemplateName());
        String prompt = promptLoader.populateTemplate(template, variables);

        StreamContext ctx = StreamRegistry.get(sessionId);
        if (ctx != null) {
            emitProgress(ctx, type.name().toLowerCase(), "RUNNING", "deepseek-chat");
        }

        AnalysisResult result = task.execute(prompt).block();

        if (ctx != null) {
            emitProgress(ctx, type.name().toLowerCase(), result.getStatus().name(), result.getModel());
            try {
                ctx.emitter().send(SseEmitter.event().name("task_result").data(result));
            } catch (IOException e) {
                log.debug("SSE send error for task_result {}: {}", type.name(), e.getMessage());
            }
        }

        return result;
    }

    private void emitProgress(StreamContext ctx, String taskName, String status, String model) {
        try {
            ctx.emitter().send(SseEmitter.event()
                    .name("task_progress")
                    .data(Map.of("task", taskName, "status", status,
                            "model", model != null ? model : "",
                            "timestamp", Instant.now().toString())));
        } catch (IOException e) {
            log.debug("SSE send progress error for {}: {}", taskName, e.getMessage());
        }
    }

    /**
     * 将审查结果发布到 GitHub PR。
     */
    public ReviewSession publishReview(String reviewId) {
        ReviewSession session = reviewRepository.findById(reviewId);
        if (session == null) {
            throw new IllegalArgumentException("Review session not found: " + reviewId);
        }

        GitHubApiService.PRUrl parsed = gitHubApiService.parsePrUrl(session.getPrUrl());
        String summary = buildPublishSummary(session);

        Long commentId = gitHubApiService.publishReview(parsed, summary).block();
        session.setStatus(SessionStatus.PUBLISHED);
        session.setPublishedCommentId(commentId);
        reviewRepository.save(session);

        return session;
    }

    private String buildPublishSummary(ReviewSession session) {
        StringBuilder sb = new StringBuilder();
        sb.append("## AI Code Review\n\n");

        AnalysisResult summaryResult = session.getAnalyses().get("summary");
        if (summaryResult != null && summaryResult.getContent() != null) {
            sb.append("### Summary\n\n").append(extractSummaryText(summaryResult.getContent())).append("\n\n");
        }

        sb.append("### Issues Overview\n\n| Severity | File | Line | Title |\n|----------|------|------|-------|\n");
        for (AnalysisResult result : session.getAnalyses().values()) {
            if (result.getIssues() != null) {
                for (Issue issue : result.getIssues()) {
                    sb.append("| ").append(issue.getSeverity()).append(" | ")
                            .append(issue.getFile() != null ? issue.getFile() : "-").append(" | ")
                            .append(issue.getLine() != null ? issue.getLine() : "-").append(" | ")
                            .append(issue.getTitle()).append(" |\n");
                }
            }
        }
        sb.append("\n---\n*Generated by [pullcat](https://github.com)*");
        return sb.toString();
    }

    private String extractSummaryText(String content) {
        try {
            String json = com.pullcat.service.llm.JsonOutputParser.extractJson(content);
            var node = new com.fasterxml.jackson.databind.ObjectMapper().readTree(json);
            return node.has("summary") ? node.get("summary").asText("") : content;
        } catch (Exception e) {
            return content;
        }
    }

    private AnalysisTask createTask(AnalysisType type) {
        String modelName = "deepseek-chat";
        switch (type) {
            case SUMMARY: return new SummaryAnalysis(chatClient, modelName);
            case RISK: return new RiskAnalysis(chatClient, modelName);
            case QUALITY: return new QualityAnalysis(chatClient, modelName);
            case CONSISTENCY: return new ConsistencyAnalysis(chatClient, modelName);
            case TESTING: return new TestingGapAnalysis(chatClient, modelName);
            default: throw new IllegalArgumentException("Unknown analysis type: " + type);
        }
    }
}
