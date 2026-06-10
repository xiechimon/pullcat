package com.pullcat.service.analysis.impl;

import com.pullcat.common.enums.AnalysisStatus;
import com.pullcat.common.enums.AnalysisType;
import com.pullcat.common.enums.SessionStatus;
import com.pullcat.dto.resp.*;
import com.pullcat.remote.GitHubApiService;
import com.pullcat.service.analysis.*;
import com.pullcat.service.llm.AnalysisTask;
import com.pullcat.toolkit.ConventionUtil;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

/**
 * 审查分析编排实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AnalysisOrchestratorImpl implements AnalysisOrchestrator {

    private final GitHubApiService gitHubApiService;
    private final PromptLoader promptLoader;
    private final ContextBuilder contextBuilder;
    private final ReviewSessionService reviewSessionService;
    private final AnalysisTaskFactory taskFactory;
    @Qualifier("analysisExecutor")
    private final ExecutorService analysisExecutor;
    private final MeterRegistry meterRegistry;
    private final ReviewPublisher reviewPublisher;

    @Override
    public void startAsync(ReviewSessionRespDTO session) {
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
                        metadata.getHeadBranch(), metadata.getHeadBranch()
                );

                sendPrInfoSSE(session.getId(), session.getPrUrl(), prData.getMetadata(), prData.getDiff());

                String discussion = fetchDiscussion(apiService, parsed);
                String relatedFiles = buildRelatedFiles(prData, enrichedPrUrl);

                final Map<String, String> finalVariables = contextBuilder.buildVariables(
                        prData.getMetadata(), prData.getFileTree(), prData.getFiles(), discussion, relatedFiles);

                GitHubApiService.PRUrl basePrUrl = new GitHubApiService.PRUrl(
                        parsed.owner(), parsed.repo(), parsed.number(),
                        metadata.getBaseBranch(), metadata.getBaseBranch()
                );
                List<String> conventionCandidates = ConventionUtil.detectConventionCandidates(prData.getFileTree());
                String repoConventions = reviewPublisher.buildConventionContent(basePrUrl, conventionCandidates);
                finalVariables.put("repo_conventions", repoConventions);

                List<AnalysisType> types = List.of(
                        AnalysisType.SUMMARY, AnalysisType.RISK, AnalysisType.QUALITY,
                        AnalysisType.CONSISTENCY, AnalysisType.TESTING
                );

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

    private GitHubApiService resolveGitHubApiService(ReviewSessionRespDTO session) {
        if (session.getInstallationId() == null) {
            return gitHubApiService;
        }
        return gitHubApiService.withInstallationToken(session.getInstallationId()).blockOptional()
                .orElse(gitHubApiService);
    }

    private String fetchDiscussion(GitHubApiService apiService, GitHubApiService.PRUrl parsed) {
        try {
            String discussion = apiService.fetchPRComments(parsed).block();
            return discussion != null ? discussion : "";
        } catch (Exception e) {
            log.warn("Failed to fetch PR comments: {}", e.getMessage());
            return "";
        }
    }

    private String buildRelatedFiles(PRDataRespDTO prData, GitHubApiService.PRUrl enrichedPrUrl) {
        try {
            List<String> allImports = new ArrayList<>();
            for (FileContentRespDTO file : prData.getFiles()) {
                allImports.addAll(contextBuilder.extractImports(file));
            }
            List<String> resolved = contextBuilder.resolveLocalImports(allImports, prData.getFileTree());
            String relatedFiles = contextBuilder.buildRelatedFilesSection(enrichedPrUrl, resolved);
            return relatedFiles != null ? relatedFiles : "";
        } catch (Exception e) {
            log.warn("Failed to build related files context: {}", e.getMessage());
            return "";
        }
    }

    private AnalysisResultRespDTO executeTask(AnalysisType type, Map<String, String> variables, String sessionId) {
        return executeTask(taskFactory.create(type), variables, sessionId);
    }

    private AnalysisResultRespDTO executeTask(AnalysisTask task, Map<String, String> variables, String sessionId) {
        String template = promptLoader.loadTemplate(task.getTemplateName());
        String prompt = promptLoader.populateTemplate(template, variables);
        AnalysisType analysisType = task.getType();

        StreamContext ctx = StreamRegistry.get(sessionId);
        if (ctx != null) {
            emitProgress(ctx, analysisType.name().toLowerCase(), "running", task.getResult().getModel());
        }

        AnalysisResultRespDTO result = task.execute(prompt).block();

        if (ctx != null) {
            emitProgress(ctx, analysisType.name().toLowerCase(), result.getStatus().name(), result.getModel());
            try {
                ctx.emitter().send(SseEmitter.event().name("task_result").data(result));
            } catch (IOException | IllegalStateException e) {
                log.debug("SSE send error for task_result {}: {}", analysisType.name(), e.getMessage());
            }
        }
        return result;
    }

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

    private void sendPrInfoSSE(String sessionId, String prUrl, PRMetadataRespDTO metadata, String diff) {
        StreamContext ctx = StreamRegistry.get(sessionId);
        if (ctx != null) {
            try {
                SsePrInfoRespDTO prInfo = new SsePrInfoRespDTO();
                prInfo.setPrUrl(prUrl);
                prInfo.setMetadata(metadata);
                prInfo.setDiff(diff != null ? diff : "");
                ctx.emitter().send(SseEmitter.event().name("pr_info").data(prInfo));
            } catch (IOException | IllegalStateException e) {
                log.debug("SSE send pr_info error: {}", e.getMessage());
            }
        }
    }

    private void sendCompletionSSE(String sessionId, String prUrl, boolean autoPublished) {
        StreamContext ctx = StreamRegistry.get(sessionId);
        if (ctx != null) {
            try {
                if (autoPublished) {
                    ctx.emitter().send(SseEmitter.event().name("auto_publish").data(new SseAutoPublishRespDTO(prUrl)));
                }
                ctx.emitter().send(SseEmitter.event().name("all_complete").data(new SseCompletionRespDTO("completed")));
                ctx.emitter().complete();
            } catch (IOException | IllegalStateException e) {
                log.debug("SSE send all_complete error: {}", e.getMessage());
            }
        }
    }

    private void sendErrorSSE(String sessionId, String message) {
        StreamContext ctx = StreamRegistry.get(sessionId);
        if (ctx != null) {
            try {
                ctx.emitter().send(SseEmitter.event().name("review_error").data(new SseMessageRespDTO(message)));
                ctx.emitter().complete();
            } catch (IOException | IllegalStateException ignored) {
            }
        }
    }
}
