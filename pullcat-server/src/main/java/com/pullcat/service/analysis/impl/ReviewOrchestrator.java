package com.pullcat.service.analysis.impl;

import com.pullcat.common.enums.AnalysisStatus;
import com.pullcat.common.enums.AnalysisType;
import com.pullcat.common.enums.SessionStatus;
import com.pullcat.dto.resp.AnalysisResultRespDTO;
import com.pullcat.dto.resp.FileContentRespDTO;
import com.pullcat.dto.resp.PRDataRespDTO;
import com.pullcat.dto.resp.PRMetadataRespDTO;
import com.pullcat.dto.resp.ReviewSessionRespDTO;
import com.pullcat.dto.resp.SseAutoPublishRespDTO;
import com.pullcat.dto.resp.SseCompletionRespDTO;
import com.pullcat.dto.resp.SseMessageRespDTO;
import com.pullcat.dto.resp.SsePrInfoRespDTO;
import com.pullcat.dto.resp.SseTaskProgressRespDTO;
import com.pullcat.remote.GitHubApiService;
import com.pullcat.toolkit.ConventionUtil;
import com.pullcat.service.analysis.AnalysisTaskFactory;
import com.pullcat.service.analysis.ContextBuilder;
import com.pullcat.service.analysis.PromptLoader;
import com.pullcat.service.analysis.ResultAggregator;
import com.pullcat.service.analysis.ReviewSessionService;
import com.pullcat.service.analysis.StreamContext;
import com.pullcat.service.analysis.StreamRegistry;
import com.pullcat.service.llm.AnalysisTask;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

/**
 * 审查编排器，负责创建审查会话、启动异步审查流程、执行分析任务并通过 SSE 实时推送进度
 */
@Slf4j
@Component
public class ReviewOrchestrator {

    private final GitHubApiService gitHubApiService;
    private final PromptLoader promptLoader;
    private final ContextBuilder contextBuilder;
    private final ReviewSessionService reviewSessionService;
    private final AnalysisTaskFactory taskFactory;
    private final ExecutorService analysisExecutor;
    private final ResultAggregator resultAggregator;
    private final MeterRegistry meterRegistry;
    private final ReviewPublisher reviewPublisher;

    public ReviewOrchestrator(GitHubApiService gitHubApiService,
                              PromptLoader promptLoader,
                              ContextBuilder contextBuilder,
                              ReviewSessionService reviewSessionService,
                              AnalysisTaskFactory taskFactory,
                              @Qualifier("analysisExecutor") ExecutorService analysisExecutor,
                              ResultAggregator resultAggregator,
                              MeterRegistry meterRegistry,
                              ReviewPublisher reviewPublisher) {
        this.gitHubApiService = gitHubApiService;
        this.promptLoader = promptLoader;
        this.contextBuilder = contextBuilder;
        this.reviewSessionService = reviewSessionService;
        this.taskFactory = taskFactory;
        this.analysisExecutor = analysisExecutor;
        this.resultAggregator = resultAggregator;
        this.meterRegistry = meterRegistry;
        this.reviewPublisher = reviewPublisher;
    }

    /**
     * 创建审查会话
     */
    public ReviewSessionRespDTO createSession(String prUrl, String userId) {
        GitHubApiService.PRUrl parsed = gitHubApiService.parsePrUrl(prUrl);

        ReviewSessionRespDTO session = new ReviewSessionRespDTO();
        session.setId(UUID.randomUUID().toString());
        session.setPrUrl(prUrl);
        session.setRepositoryFullName(parsed.owner() + "/" + parsed.repo());
        session.setStatus(SessionStatus.FETCHING);
        session.setUserId(userId);
        return session;
    }

    /**
     * 启动异步审查流程，拉取 PR 数据、构建上下文、执行分析并推送 SSE 事件
     */
    public void startReviewAsync(ReviewSessionRespDTO session) {
        SecurityContext securityContext = SecurityContextHolder.getContext();
        analysisExecutor.submit(() -> {
            SecurityContextHolder.setContext(securityContext);
            Timer.Sample sample = Timer.start(meterRegistry);
            try {
                GitHubApiService apiService = resolveGitHubApiService(session);
                GitHubApiService.PRUrl parsed = apiService.parsePrUrl(session.getPrUrl());

                PRDataRespDTO prData = apiService.fetchPRData(parsed).block();
                Objects.requireNonNull(prData, "PR data must not be null");
                PRMetadataRespDTO metadata = prData.getMetadata();
                session.setPrMetadata(metadata);
                session.setRawDiff(prData.getDiff());
                session.setStatus(SessionStatus.ANALYZING);
                session.setRepositoryFullName(parsed.owner() + "/" + parsed.repo());
                reviewSessionService.save(session);

                GitHubApiService.PRUrl enrichedPrUrl = new GitHubApiService.PRUrl(
                        parsed.owner(), parsed.repo(), parsed.number(),
                        metadata.getHeadBranch(), metadata.getHeadBranch());

                sendPrInfoSSE(session.getId(), session.getPrUrl(), prData.getMetadata(), prData.getDiff());

                contextBuilder.buildVariables(prData.getMetadata(), prData.getFileTree(), prData.getFiles());

                String discussion = fetchDiscussion(apiService, parsed);

                String relatedFiles = buildRelatedFiles(prData, enrichedPrUrl);

                final Map<String, String> finalVariables = contextBuilder.buildVariables(
                        prData.getMetadata(), prData.getFileTree(), prData.getFiles(), discussion, relatedFiles);

                GitHubApiService.PRUrl basePrUrl = new GitHubApiService.PRUrl(
                        parsed.owner(), parsed.repo(), parsed.number(),
                        metadata.getBaseBranch(), metadata.getBaseBranch());
                List<String> conventionCandidates = ConventionUtil.detectConventionCandidates(prData.getFileTree());
                String repoConventions = reviewPublisher.buildConventionContent(basePrUrl, conventionCandidates);
                finalVariables.put("repo_conventions", repoConventions);

                List<AnalysisType> types = List.of(
                        AnalysisType.SUMMARY, AnalysisType.RISK, AnalysisType.QUALITY,
                        AnalysisType.CONSISTENCY, AnalysisType.TESTING);

                List<CompletableFuture<AnalysisResultRespDTO>> futures = types.stream()
                        .map(type -> CompletableFuture
                                .supplyAsync(() -> executeTask(type, finalVariables, session.getId()), analysisExecutor)
                                .exceptionally(ex -> {
                                    log.error("Task {} failed unexpectedly: {}", type, ex.getMessage());
                                    AnalysisResultRespDTO failed = new AnalysisResultRespDTO(type);
                                    failed.setStatus(AnalysisStatus.FAILED);
                                    failed.setErrorMessage(ex.getMessage());
                                    failed.setCompletedAt(Instant.now());
                                    return failed;
                                }))
                        .toList();

                CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

                for (int i = 0; i < types.size(); i++) {
                    try {
                        AnalysisResultRespDTO result = futures.get(i).get();
                        session.getAnalyses().put(types.get(i).name().toLowerCase(), result);
                    } catch (Exception e) {
                        log.error("Failed to get result for {}: {}", types.get(i), e.getMessage());
                        AnalysisResultRespDTO failed = new AnalysisResultRespDTO(types.get(i));
                        failed.setStatus(AnalysisStatus.FAILED);
                        failed.setErrorMessage(e.getMessage());
                        session.getAnalyses().put(types.get(i).name().toLowerCase(), failed);
                    }
                }

                long completedCount = session.getAnalyses().values().stream()
                        .filter(result -> result.getStatus() == AnalysisStatus.COMPLETED)
                        .count();
                session.setStatus(completedCount > 0 ? SessionStatus.COMPLETED : SessionStatus.FAILED);
                session.setCompletedAt(Instant.now());
                reviewSessionService.save(session);

                boolean autoPublished = reviewPublisher.tryAutoPublish(session);

                sample.stop(Timer.builder("reviews_duration_seconds")
                        .description("Duration of PR review analysis")
                        .register(meterRegistry));
                meterRegistry.counter("reviews_total", "status", session.getStatus().name()).increment();

                for (AnalysisResultRespDTO analysisResult : session.getAnalyses().values()) {
                    if (analysisResult.getStatus() == AnalysisStatus.COMPLETED) {
                        meterRegistry.counter("llm_requests_total",
                                "model", analysisResult.getModel() != null ? analysisResult.getModel() : "unknown",
                                "status", "success").increment();
                    } else {
                        meterRegistry.counter("llm_requests_total", "model", "unknown", "status", "failed").increment();
                    }
                }

                sendCompletionSSE(session.getId(), session.getPrUrl(), autoPublished);
            } catch (Exception e) {
                log.error("Review failed: {}", e.getMessage(), e);
                session.setStatus(SessionStatus.FAILED);
                reviewSessionService.save(session);

                sample.stop(Timer.builder("reviews_duration_seconds")
                        .description("Duration of PR review analysis")
                        .register(meterRegistry));
                meterRegistry.counter("reviews_total", "status", SessionStatus.FAILED.name()).increment();
                sendErrorSSE(session.getId(), e.getMessage());
            } finally {
                SecurityContextHolder.clearContext();
            }
        });
    }

    /**
     * 根据会话确定使用的 GitHubApiService 实例
     */
    private GitHubApiService resolveGitHubApiService(ReviewSessionRespDTO session) {
        if (session.getInstallationId() == null) {
            return gitHubApiService;
        }
        return gitHubApiService.withInstallationToken(session.getInstallationId()).blockOptional()
                .orElse(gitHubApiService);
    }

    /**
     * 拉取 PR 讨论内容
     */
    private String fetchDiscussion(GitHubApiService apiService, GitHubApiService.PRUrl parsed) {
        String discussion = "";
        try {
            discussion = apiService.fetchPRComments(parsed).block();
            if (discussion == null) {
                discussion = "";
            }
        } catch (Exception e) {
            log.warn("Failed to fetch PR comments: {}", e.getMessage());
        }
        return discussion;
    }

    /**
     * 构建相关依赖文件上下文
     */
    private String buildRelatedFiles(PRDataRespDTO prData, GitHubApiService.PRUrl enrichedPrUrl) {
        String relatedFiles = "";
        try {
            List<String> allImports = new ArrayList<>();
            for (FileContentRespDTO file : prData.getFiles()) {
                allImports.addAll(contextBuilder.extractImports(file));
            }
            List<String> resolved = contextBuilder.resolveLocalImports(allImports, prData.getFileTree());
            relatedFiles = contextBuilder.buildRelatedFilesSection(enrichedPrUrl, resolved);
            if (relatedFiles == null) {
                relatedFiles = "";
            }
        } catch (Exception e) {
            log.warn("Failed to build related files context: {}", e.getMessage());
        }
        return relatedFiles;
    }

    /**
     * 推送 PR 信息 SSE 事件
     */
    private void sendPrInfoSSE(String sessionId, String prUrl, PRMetadataRespDTO metadata, String diff) {
        StreamContext ctx = StreamRegistry.get(sessionId);
        if (ctx != null) {
            try {
                SsePrInfoRespDTO prInfo = new SsePrInfoRespDTO();
                prInfo.setPrUrl(prUrl);
                prInfo.setMetadata(metadata);
                prInfo.setDiff(diff != null ? diff : "");
                ctx.emitter().send(SseEmitter.event()
                        .name("pr_info")
                        .data(prInfo));
            } catch (IOException | IllegalStateException e) {
                log.debug("SSE send pr_info error: {}", e.getMessage());
            }
        }
    }

    /**
     * 推送审查完成 SSE 事件
     */
    private void sendCompletionSSE(String sessionId, String prUrl, boolean autoPublished) {
        StreamContext ctx = StreamRegistry.get(sessionId);
        if (ctx != null) {
            try {
                if (autoPublished) {
                    ctx.emitter().send(SseEmitter.event()
                            .name("auto_publish")
                            .data(new SseAutoPublishRespDTO(prUrl)));
                }
                ctx.emitter().send(SseEmitter.event().name("all_complete")
                        .data(new SseCompletionRespDTO("completed")));
                ctx.emitter().complete();
            } catch (IOException | IllegalStateException e) {
                log.debug("SSE send all_complete error: {}", e.getMessage());
            }
        }
    }

    /**
     * 推送审查错误 SSE 事件
     */
    private void sendErrorSSE(String sessionId, String message) {
        StreamContext ctx = StreamRegistry.get(sessionId);
        if (ctx != null) {
            try {
                ctx.emitter().send(SseEmitter.event().name("review_error")
                        .data(new SseMessageRespDTO(message)));
                ctx.emitter().complete();
            } catch (IOException | IllegalStateException ignored) {
            }
        }
    }

    /**
     * 按分析类型执行任务
     */
    private AnalysisResultRespDTO executeTask(AnalysisType type, Map<String, String> variables, String sessionId) {
        AnalysisTask task = createTask(type);
        return executeTask(task, variables, sessionId);
    }

    /**
     * 执行分析任务并推送进度
     */
    AnalysisResultRespDTO executeTask(AnalysisTask task, Map<String, String> variables, String sessionId) {
        String template = promptLoader.loadTemplate(task.getTemplateName());
        String prompt = promptLoader.populateTemplate(template, variables);
        AnalysisType type = task.getType();

        StreamContext ctx = StreamRegistry.get(sessionId);
        if (ctx != null) {
            emitProgress(ctx, type.name().toLowerCase(), "running", task.getResult().getModel());
        }

        AnalysisResultRespDTO result = task.execute(prompt).block();

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

    /**
     * 推送任务进度 SSE 事件
     */
    private void emitProgress(StreamContext ctx, String taskName, String status, String model) {
        try {
            SseTaskProgressRespDTO progress = new SseTaskProgressRespDTO();
            progress.setTask(taskName);
            progress.setStatus(status);
            progress.setModel(model != null ? model : "");
            progress.setTimestamp(Instant.now().toString());
            ctx.emitter().send(SseEmitter.event().name("task_progress").data(progress));
        } catch (IOException | IllegalStateException e) {
            log.debug("SSE send progress error for {}: {}", taskName, e.getMessage());
        }
    }

    /**
     * 创建分析任务
     */
    private AnalysisTask createTask(AnalysisType type) {
        return taskFactory.create(type);
    }
}
