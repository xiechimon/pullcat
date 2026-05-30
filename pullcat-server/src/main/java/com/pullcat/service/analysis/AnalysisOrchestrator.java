package com.pullcat.service.analysis;

import com.pullcat.model.*;
import com.pullcat.service.github.GitHubApiService;
import com.pullcat.service.llm.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

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
    private final ChatClient lightChatClient;
    private final ChatClient heavyChatClient;
    private final ExecutorService analysisExecutor;
    private final ResultAggregator resultAggregator;

    @Value("${pullcat.llm.light-model:deepseek-chat}")
    private String lightModelName;

    @Value("${pullcat.llm.heavy-model:deepseek-reasoner}")
    private String heavyModelName;

    public AnalysisOrchestrator(GitHubApiService gitHubApiService,
                                PromptLoader promptLoader,
                                ContextBuilder contextBuilder,
                                ReviewRepository reviewRepository,
                                @Qualifier("lightChatClient") ChatClient lightChatClient,
                                @Qualifier("heavyChatClient") ChatClient heavyChatClient,
                                @Qualifier("analysisExecutor") ExecutorService analysisExecutor,
                                ResultAggregator resultAggregator) {
        this.gitHubApiService = gitHubApiService;
        this.promptLoader = promptLoader;
        this.contextBuilder = contextBuilder;
        this.reviewRepository = reviewRepository;
        this.lightChatClient = lightChatClient;
        this.heavyChatClient = heavyChatClient;
        this.analysisExecutor = analysisExecutor;
        this.resultAggregator = resultAggregator;
    }

    /**
     * 创建审查会话，仅解析 URL 并保存初始状态到 Redis，不启动分析。
     */
    public ReviewSession createSession(String prUrl) {
        GitHubApiService.PRUrl parsed = gitHubApiService.parsePrUrl(prUrl);

        ReviewSession session = new ReviewSession();
        session.setId(UUID.randomUUID().toString());
        session.setPrUrl(prUrl);
        session.setRepositoryFullName(parsed.owner() + "/" + parsed.repo());
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
                session.setRawDiff(prData.getDiff());
                session.setStatus(SessionStatus.ANALYZING);
                session.setRepositoryFullName(parsed.owner() + "/" + parsed.repo());
                reviewRepository.save(session);

                StreamContext metaCtx = StreamRegistry.get(session.getId());
                if (metaCtx != null) {
                    try {
                        metaCtx.emitter().send(SseEmitter.event()
                                .name("pr_info")
                                .data(Map.of(
                                        "prUrl", session.getPrUrl(),
                                        "metadata", prData.getMetadata(),
                                        "diff", prData.getDiff() != null ? prData.getDiff() : "")));
                    } catch (IOException | IllegalStateException e) {
                        log.debug("SSE send pr_info error: {}", e.getMessage());
                    }
                }

                Map<String, String> variables = contextBuilder.buildVariables(
                        prData.getMetadata(), prData.getFileTree(), prData.getFiles());

                String discussion = "";
                String relatedFiles = "";
                try {
                    discussion = gitHubApiService.fetchPRComments(parsed).block();
                    if (discussion == null) discussion = "";
                } catch (Exception e) {
                    log.debug("Failed to fetch PR comments: {}", e.getMessage());
                }
                try {
                    List<String> allImports = new ArrayList<>();
                    for (FileContent file : prData.getFiles()) {
                        allImports.addAll(contextBuilder.extractImports(file));
                    }
                    List<String> resolved = contextBuilder.resolveLocalImports(allImports, prData.getFileTree());
                    relatedFiles = contextBuilder.buildRelatedFilesSection(parsed, resolved);
                    if (relatedFiles == null) relatedFiles = "";
                } catch (Exception e) {
                    log.debug("Failed to build related files context: {}", e.getMessage());
                }

                final Map<String, String> finalVariables = contextBuilder.buildVariables(
                        prData.getMetadata(), prData.getFileTree(), prData.getFiles(),
                        discussion, relatedFiles);

                List<AnalysisType> types = List.of(
                        AnalysisType.SUMMARY, AnalysisType.RISK, AnalysisType.QUALITY,
                        AnalysisType.CONSISTENCY, AnalysisType.TESTING);

                List<CompletableFuture<AnalysisResult>> futures = types.stream()
                        .map(type -> CompletableFuture
                                .supplyAsync(() -> executeTask(type, finalVariables, session.getId()), analysisExecutor)
                                .exceptionally(ex -> {
                                    log.error("Task {} failed unexpectedly: {}", type, ex.getMessage());
                                    AnalysisResult failed = new AnalysisResult(type);
                                    failed.setStatus(AnalysisStatus.FAILED);
                                    failed.setErrorMessage(ex.getMessage());
                                    failed.setCompletedAt(Instant.now());
                                    return failed;
                                }))
                        .toList();

                CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

                for (int i = 0; i < types.size(); i++) {
                    try {
                        AnalysisResult result = futures.get(i).get();
                        session.getAnalyses().put(types.get(i).name().toLowerCase(), result);
                    } catch (Exception e) {
                        log.error("Failed to get result for {}: {}", types.get(i), e.getMessage());
                        AnalysisResult failed = new AnalysisResult(types.get(i));
                        failed.setStatus(AnalysisStatus.FAILED);
                        failed.setErrorMessage(e.getMessage());
                        session.getAnalyses().put(types.get(i).name().toLowerCase(), failed);
                    }
                }

                session.setStatus(SessionStatus.COMPLETED);
                session.setCompletedAt(Instant.now());
                reviewRepository.save(session);

                StreamContext finalCtx = StreamRegistry.get(session.getId());
                if (finalCtx != null) {
                    try {
                        finalCtx.emitter().send(SseEmitter.event().name("all_complete").data(Map.of("status", "completed")));
                        finalCtx.emitter().complete();
                    } catch (IOException | IllegalStateException e) {
                        log.debug("SSE send all_complete error: {}", e.getMessage());
                    }
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
                    } catch (IOException | IllegalStateException ignored) {}
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
            emitProgress(ctx, type.name().toLowerCase(), "running", task.getResult().getModel());
        }

        AnalysisResult result = task.execute(prompt).block();

        if (ctx != null) {
            emitProgress(ctx, type.name().toLowerCase(), result.getStatus().name(), result.getModel());
            try {
                ctx.emitter().send(SseEmitter.event().name("task_result").data(result));
            } catch (IOException | IllegalStateException e) {
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
        } catch (IOException | IllegalStateException e) {
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

        List<AnalysisResult> allResults = new ArrayList<>(session.getAnalyses().values());
        List<Issue> dedupedIssues = resultAggregator.mergeResults(allResults);

        sb.append("### Issues Overview (").append(dedupedIssues.size()).append(" unique)\n\n");
        sb.append("| Severity | File | Line | Title |\n|----------|------|------|-------|\n");
        for (Issue issue : dedupedIssues) {
            sb.append("| ").append(issue.getSeverity()).append(" | ")
                    .append(issue.getFile() != null ? issue.getFile() : "-").append(" | ")
                    .append(issue.getLine() != null ? issue.getLine() : "-").append(" | ")
                    .append(issue.getTitle()).append(" |\n");
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
        switch (type) {
            case SUMMARY: return new SummaryAnalysis(lightChatClient, lightModelName);
            case RISK: return new RiskAnalysis(heavyChatClient, heavyModelName);
            case QUALITY: return new QualityAnalysis(heavyChatClient, heavyModelName);
            case CONSISTENCY: return new ConsistencyAnalysis(heavyChatClient, heavyModelName);
            case TESTING: return new TestingGapAnalysis(lightChatClient, lightModelName);
            default: throw new IllegalArgumentException("Unknown analysis type: " + type);
        }
    }
}
