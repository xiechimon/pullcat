package com.pullcat.service.analysis;

import com.pullcat.model.*;
import com.pullcat.service.github.GitHubApiService;
import com.pullcat.service.llm.*;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
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
    private final RuleEngine ruleEngine;
    private final RuleRepository ruleRepository;
    private final RuleSuggestionService ruleSuggestionService;
    private final MeterRegistry meterRegistry;

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
                                ResultAggregator resultAggregator,
                                RuleEngine ruleEngine,
                                RuleRepository ruleRepository,
                                RuleSuggestionService ruleSuggestionService,
                                MeterRegistry meterRegistry) {
        this.gitHubApiService = gitHubApiService;
        this.promptLoader = promptLoader;
        this.contextBuilder = contextBuilder;
        this.reviewRepository = reviewRepository;
        this.lightChatClient = lightChatClient;
        this.heavyChatClient = heavyChatClient;
        this.analysisExecutor = analysisExecutor;
        this.resultAggregator = resultAggregator;
        this.ruleEngine = ruleEngine;
        this.ruleRepository = ruleRepository;
        this.ruleSuggestionService = ruleSuggestionService;
        this.meterRegistry = meterRegistry;
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
            Timer.Sample sample = Timer.start(meterRegistry);
            try {
                GitHubApiService.PRUrl parsed = gitHubApiService.parsePrUrl(session.getPrUrl());

                PRData prData = gitHubApiService.fetchPRData(parsed).block();
                PRMetadata metadata = prData.getMetadata();
                session.setPrMetadata(metadata);
                session.setRawDiff(prData.getDiff());
                session.setStatus(SessionStatus.ANALYZING);
                session.setRepositoryFullName(parsed.owner() + "/" + parsed.repo());
                reviewRepository.save(session);

                GitHubApiService.PRUrl enrichedPrUrl = new GitHubApiService.PRUrl(
                        parsed.owner(), parsed.repo(), parsed.number(),
                        metadata.getHeadBranch(), metadata.getHeadBranch());

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
                    log.warn("Failed to fetch PR comments: {}", e.getMessage());
                }
                try {
                    List<String> allImports = new ArrayList<>();
                    for (FileContent file : prData.getFiles()) {
                        allImports.addAll(contextBuilder.extractImports(file));
                    }
                    List<String> resolved = contextBuilder.resolveLocalImports(allImports, prData.getFileTree());
                    relatedFiles = contextBuilder.buildRelatedFilesSection(enrichedPrUrl, resolved);
                    if (relatedFiles == null) relatedFiles = "";
                } catch (Exception e) {
                    log.warn("Failed to build related files context: {}", e.getMessage());
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

                long completedCount = session.getAnalyses().values().stream()
                        .filter(r -> r.getStatus() == AnalysisStatus.COMPLETED)
                        .count();
                session.setStatus(completedCount > 0 ? SessionStatus.COMPLETED : SessionStatus.FAILED);
                session.setCompletedAt(Instant.now());
                reviewRepository.save(session);

                sample.stop(Timer.builder("reviews_duration_seconds")
                        .description("Duration of PR review analysis")
                        .register(meterRegistry));
                meterRegistry.counter("reviews_total",
                        "status", session.getStatus().name()).increment();

                for (AnalysisResult analysisResult : session.getAnalyses().values()) {
                    if (analysisResult.getStatus() == AnalysisStatus.COMPLETED) {
                        meterRegistry.counter("llm_requests_total",
                                "model", analysisResult.getModel() != null ? analysisResult.getModel() : "unknown",
                                "status", "success").increment();
                    } else {
                        meterRegistry.counter("llm_requests_total",
                                "model", "unknown",
                                "status", "failed").increment();
                    }
                }

                StreamContext finalCtx = StreamRegistry.get(session.getId());
                if (finalCtx != null) {
                    try {
                        finalCtx.emitter().send(SseEmitter.event().name("all_complete").data(Map.of("status", "completed")));
                        checkAndNotifyRuleSuggestions(session, finalCtx);
                        finalCtx.emitter().complete();
                    } catch (IOException | IllegalStateException e) {
                        log.debug("SSE send all_complete error: {}", e.getMessage());
                    }
                }
            } catch (Exception e) {
                log.error("Review failed: {}", e.getMessage(), e);
                session.setStatus(SessionStatus.FAILED);
                reviewRepository.save(session);

                sample.stop(Timer.builder("reviews_duration_seconds")
                        .description("Duration of PR review analysis")
                        .register(meterRegistry));
                meterRegistry.counter("reviews_total",
                        "status", SessionStatus.FAILED.name()).increment();
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

        List<AnalysisResult> allResults = new ArrayList<>(session.getAnalyses().values());
        List<Issue> dedupedIssues = resultAggregator.mergeResults(allResults);

        String summary = buildPublishSummary(dedupedIssues, session);

        List<GitHubApiService.ReviewComment> comments = dedupedIssues.stream()
                .filter(i -> i.getSuggestionCode() != null && !i.getSuggestionCode().isBlank())
                .filter(i -> i.getFile() != null && i.getLine() != null)
                .map(i -> new GitHubApiService.ReviewComment(
                        i.getFile(), i.getLine(), buildSuggestionBlock(i)))
                .toList();

        Long commentId = gitHubApiService.publishReviewWithComments(parsed, summary, comments).block();
        session.setStatus(SessionStatus.PUBLISHED);
        session.setPublishedCommentId(commentId);
        reviewRepository.save(session);

        return session;
    }

    private String buildPublishSummary(List<Issue> dedupedIssues, ReviewSession session) {
        StringBuilder sb = new StringBuilder();
        sb.append("## AI 代码审查\n\n");

        AnalysisResult summaryResult = session.getAnalyses().get("summary");
        if (summaryResult != null && summaryResult.getContent() != null) {
            sb.append("### 审查摘要\n\n").append(extractSummaryText(summaryResult.getContent())).append("\n\n");
        }

        sb.append("### 问题概览（").append(dedupedIssues.size()).append(" 个）\n\n");
        sb.append("| 严重度 | 文件 | 行号 | 问题 |\n|--------|------|------|------|\n");
        for (Issue issue : dedupedIssues) {
            sb.append("| ").append(issue.getSeverity()).append(" | ")
                    .append(issue.getFile() != null ? issue.getFile() : "-").append(" | ")
                    .append(issue.getLine() != null ? issue.getLine() : "-").append(" | ")
                    .append(issue.getTitle()).append(" |\n");
        }

        long fixCount = dedupedIssues.stream().filter(i -> i.getSuggestionCode() != null
                && !i.getSuggestionCode().isBlank() && i.getFile() != null && i.getLine() != null).count();
        if (fixCount > 0) {
            sb.append("\n### 修复建议（").append(fixCount).append(" 条内联评论）\n\n");
            sb.append("代码修复建议已作为 inline comment 发布到对应行，可直接在 PR Files Changed 页面查看并一键应用\n\n");
        }

        sb.append("\n---\n*由 [pullcat](https://xmon.me) 自动生成*");
        return sb.toString();
    }

    String buildSuggestionBlock(Issue issue) {
        StringBuilder sb = new StringBuilder();
        sb.append("**[").append(issue.getSeverity()).append("] ")
                .append(issue.getTitle()).append("**\n\n");
        sb.append(issue.getDescription()).append("\n\n");
        sb.append("```suggestion\n");
        sb.append(issue.getSuggestionCode());
        sb.append("\n```\n");
        return sb.toString();
    }

    private void checkAndNotifyRuleSuggestions(ReviewSession session, StreamContext ctx) {
        try {
            String fullName = session.getRepositoryFullName();
            if (fullName == null) return;
            String[] parts = fullName.split("/", 2);
            if (parts.length != 2) return;

            boolean hasSuggestions = ruleSuggestionService.hasNewSuggestions(parts[0], parts[1]);
            if (hasSuggestions) {
                ctx.emitter().send(SseEmitter.event()
                        .name("rule_suggestion")
                        .data(Map.of("message", "发现潜在规则建议",
                                "url", "/settings/repos/" + parts[0] + "/" + parts[1])));
            }
        } catch (Exception e) {
            log.debug("Failed to check rule suggestions: {}", e.getMessage());
        }
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
