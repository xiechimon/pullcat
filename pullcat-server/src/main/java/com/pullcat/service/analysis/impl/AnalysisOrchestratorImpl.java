package com.pullcat.service.analysis.impl;

import com.pullcat.common.enums.AnalysisStatus;
import com.pullcat.common.enums.AnalysisType;
import com.pullcat.common.enums.SessionStatus;
import com.pullcat.dto.req.PublishReqDTO;
import com.pullcat.dto.resp.AnalysisResultRespDTO;
import com.pullcat.dto.resp.FileContentRespDTO;
import com.pullcat.dto.resp.IssueRespDTO;
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
import com.pullcat.service.analysis.AnalysisOrchestrator;
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
import org.springframework.stereotype.Service;
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
 * 分析编排器，负责完整的 PR 审查流程：拉取数据、构建上下文、执行五个分析任务，
 * 并通过 SSE 实时推送进度
 */
@Slf4j
@Service
public class AnalysisOrchestratorImpl implements AnalysisOrchestrator {

    private final GitHubApiService gitHubApiService;
    private final PromptLoader promptLoader;
    private final ContextBuilder contextBuilder;
    private final ReviewSessionService reviewSessionService;
    private final AnalysisTaskFactory taskFactory;
    private final ExecutorService analysisExecutor;
    private final ResultAggregator resultAggregator;
    private final MeterRegistry meterRegistry;

    public AnalysisOrchestratorImpl(GitHubApiService gitHubApiService,
                                    PromptLoader promptLoader,
                                    ContextBuilder contextBuilder,
                                    ReviewSessionService reviewSessionService,
                                    AnalysisTaskFactory taskFactory,
                                    @Qualifier("analysisExecutor") ExecutorService analysisExecutor,
                                    ResultAggregator resultAggregator,
                                    MeterRegistry meterRegistry) {
        this.gitHubApiService = gitHubApiService;
        this.promptLoader = promptLoader;
        this.contextBuilder = contextBuilder;
        this.reviewSessionService = reviewSessionService;
        this.taskFactory = taskFactory;
        this.analysisExecutor = analysisExecutor;
        this.resultAggregator = resultAggregator;
        this.meterRegistry = meterRegistry;
    }

    @Override
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

    @Override
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

                StreamContext metaCtx = StreamRegistry.get(session.getId());
                if (metaCtx != null) {
                    try {
                        SsePrInfoRespDTO prInfo = new SsePrInfoRespDTO();
                        prInfo.setPrUrl(session.getPrUrl());
                        prInfo.setMetadata(prData.getMetadata());
                        prInfo.setDiff(prData.getDiff() != null ? prData.getDiff() : "");
                        metaCtx.emitter().send(SseEmitter.event()
                                .name("pr_info")
                                .data(prInfo));
                    } catch (IOException | IllegalStateException e) {
                        log.debug("SSE send pr_info error: {}", e.getMessage());
                    }
                }

                contextBuilder.buildVariables(prData.getMetadata(), prData.getFileTree(), prData.getFiles());

                String discussion = "";
                String relatedFiles = "";
                try {
                    discussion = apiService.fetchPRComments(parsed).block();
                    if (discussion == null) {
                        discussion = "";
                    }
                } catch (Exception e) {
                    log.warn("Failed to fetch PR comments: {}", e.getMessage());
                }
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

                final Map<String, String> finalVariables = contextBuilder.buildVariables(
                        prData.getMetadata(), prData.getFileTree(), prData.getFiles(), discussion, relatedFiles);

                // 使用 base branch 拉取约定文件，确保获取目标分支（main/master/develop 等）而非 feature branch
                GitHubApiService.PRUrl basePrUrl = new GitHubApiService.PRUrl(
                        parsed.owner(), parsed.repo(), parsed.number(),
                        metadata.getBaseBranch(), metadata.getBaseBranch());
                List<String> conventionCandidates = ConventionUtil.detectConventionCandidates(prData.getFileTree());
                String repoConventions = buildConventionContent(basePrUrl, conventionCandidates, apiService);
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

                boolean autoPublished = tryAutoPublish(session);

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

                StreamContext finalCtx = StreamRegistry.get(session.getId());
                if (finalCtx != null) {
                    try {
                        if (autoPublished) {
                            finalCtx.emitter().send(SseEmitter.event()
                                    .name("auto_publish")
                                    .data(new SseAutoPublishRespDTO(session.getPrUrl())));
                        }
                        finalCtx.emitter().send(SseEmitter.event().name("all_complete")
                                .data(new SseCompletionRespDTO("completed")));
                        finalCtx.emitter().complete();
                    } catch (IOException | IllegalStateException e) {
                        log.debug("SSE send all_complete error: {}", e.getMessage());
                    }
                }
            } catch (Exception e) {
                log.error("Review failed: {}", e.getMessage(), e);
                session.setStatus(SessionStatus.FAILED);
                reviewSessionService.save(session);

                sample.stop(Timer.builder("reviews_duration_seconds")
                        .description("Duration of PR review analysis")
                        .register(meterRegistry));
                meterRegistry.counter("reviews_total", "status", SessionStatus.FAILED.name()).increment();
                StreamContext finalCtx = StreamRegistry.get(session.getId());
                if (finalCtx != null) {
                    try {
                        finalCtx.emitter().send(SseEmitter.event().name("review_error")
                                .data(new SseMessageRespDTO(e.getMessage())));
                        finalCtx.emitter().complete();
                    } catch (IOException | IllegalStateException ignored) {
                    }
                }
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

    private AnalysisResultRespDTO executeTask(AnalysisType type, Map<String, String> variables, String sessionId) {
        AnalysisTask task = createTask(type);
        return executeTask(task, variables, sessionId);
    }

    private AnalysisResultRespDTO executeTask(AnalysisTask task, Map<String, String> variables, String sessionId) {
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

    @Override
    public ReviewSessionRespDTO publishReview(String reviewId, PublishReqDTO requestParam) {
        ReviewSessionRespDTO session = reviewSessionService.findById(reviewId);
        if (session == null) {
            throw new IllegalArgumentException("Review session not found: " + reviewId);
        }

        GitHubApiService apiService = resolveGitHubApiService(session);
        GitHubApiService.PRUrl parsed = apiService.parsePrUrl(session.getPrUrl());

        List<AnalysisResultRespDTO> allResults = new ArrayList<>(session.getAnalyses().values());
        List<IssueRespDTO> dedupedIssues = resultAggregator.mergeResults(allResults);
        if (requestParam.getSelectedIssueIds() != null && !requestParam.getSelectedIssueIds().isEmpty()) {
            dedupedIssues = dedupedIssues.stream()
                    .filter(issue -> requestParam.getSelectedIssueIds().contains(issue.getId()))
                    .toList();
        }

        String summary = buildPublishSummary(dedupedIssues, session);
        if (!requestParam.isIncludeSummary()) {
            summary = buildIssuesOnlySummary(dedupedIssues);
        }

        List<GitHubApiService.ReviewComment> comments = dedupedIssues.stream()
                .filter(issue -> issue.getSuggestionCode() != null && !issue.getSuggestionCode().isBlank())
                .filter(issue -> issue.getFile() != null && issue.getLine() != null)
                .map(issue -> new GitHubApiService.ReviewComment(
                        issue.getFile(), issue.getLine(), buildSuggestionBlock(issue)))
                .toList();

        Long commentId = apiService.publishReviewWithComments(parsed, summary, comments).block();
        session.setStatus(SessionStatus.PUBLISHED);
        session.setPublishedCommentId(commentId);
        reviewSessionService.save(session);

        return session;
    }

    private String buildIssuesOnlySummary(List<IssueRespDTO> dedupedIssues) {
        StringBuilder sb = new StringBuilder();
        sb.append("## AI 代码审查\n\n");
        sb.append("### 问题概览（").append(dedupedIssues.size()).append(" 个）\n\n");
        sb.append("| 严重度 | 文件 | 行号 | 问题 |\n|--------|------|------|------|\n");
        for (IssueRespDTO issue : dedupedIssues) {
            sb.append("| ").append(issue.getSeverity()).append(" | ")
                    .append(issue.getFile() != null ? issue.getFile() : "-").append(" | ")
                    .append(issue.getLine() != null ? issue.getLine() : "-").append(" | ")
                    .append(issue.getTitle()).append(" |\n");
        }
        sb.append("\n---\n*由 [pullcat](https://xmon.me) 自动生成*");
        return sb.toString();
    }

    private String buildPublishSummary(List<IssueRespDTO> dedupedIssues, ReviewSessionRespDTO session) {
        StringBuilder sb = new StringBuilder();
        sb.append("## AI 代码审查\n\n");

        AnalysisResultRespDTO summaryResult = session.getAnalyses().get("summary");
        if (summaryResult != null && summaryResult.getContent() != null) {
            sb.append("### 审查摘要\n\n").append(extractSummaryText(summaryResult.getContent())).append("\n\n");
        }

        sb.append("### 问题概览（").append(dedupedIssues.size()).append(" 个）\n\n");
        sb.append("| 严重度 | 文件 | 行号 | 问题 |\n|--------|------|------|------|\n");
        for (IssueRespDTO issue : dedupedIssues) {
            sb.append("| ").append(issue.getSeverity()).append(" | ")
                    .append(issue.getFile() != null ? issue.getFile() : "-").append(" | ")
                    .append(issue.getLine() != null ? issue.getLine() : "-").append(" | ")
                    .append(issue.getTitle()).append(" |\n");
        }

        long fixCount = dedupedIssues.stream()
                .filter(issue -> issue.getSuggestionCode() != null && !issue.getSuggestionCode().isBlank()
                        && issue.getFile() != null && issue.getLine() != null)
                .count();
        if (fixCount > 0) {
            sb.append("\n### 修复建议（").append(fixCount).append(" 条内联评论）\n\n");
            sb.append("代码修复建议已作为 inline comment 发布到对应行，可直接在 PR Files Changed 页面查看并一键应用\n\n");
        }

        sb.append("\n---\n*由 [pullcat](https://xmon.me) 自动生成*");
        return sb.toString();
    }

    String buildSuggestionBlock(IssueRespDTO issue) {
        StringBuilder sb = new StringBuilder();
        sb.append("**[").append(issue.getSeverity()).append("] ").append(issue.getTitle()).append("**\n\n");
        sb.append(issue.getDescription()).append("\n\n");
        sb.append("```suggestion\n");
        sb.append(issue.getSuggestionCode());
        sb.append("\n```\n");
        return sb.toString();
    }

    private boolean tryAutoPublish(ReviewSessionRespDTO session) {
        String fullName = session.getRepositoryFullName();
        if (fullName == null) {
            return false;
        }
        String[] parts = fullName.split("/", 2);
        if (parts.length != 2) {
            return false;
        }

        if (reviewSessionService.isAutoPublishEnabled(parts[0], parts[1])) {
            try {
                publishAutoReview(session);
                log.info("Auto-published review {} to PR {}", session.getId(), session.getPrUrl());
                return true;
            } catch (Exception e) {
                log.error("Auto-publish failed for review {}: {}", session.getId(), e.getMessage());
            }
        }
        return false;
    }

    private void publishAutoReview(ReviewSessionRespDTO session) {
        GitHubApiService apiService = resolveGitHubApiService(session);
        GitHubApiService.PRUrl parsed = apiService.parsePrUrl(session.getPrUrl());
        List<AnalysisResultRespDTO> allResults = new ArrayList<>(session.getAnalyses().values());
        List<IssueRespDTO> dedupedIssues = resultAggregator.mergeResults(allResults);
        String summary = buildPublishSummary(dedupedIssues, session);
        apiService.publishReview(parsed, summary).block();
        session.setStatus(SessionStatus.PUBLISHED);
        reviewSessionService.save(session);
    }

    private String extractSummaryText(String content) {
        try {
            String json = com.pullcat.toolkit.JsonOutputParser.extractJson(content);
            var node = new com.fasterxml.jackson.databind.ObjectMapper().readTree(json);
            return node.has("summary") ? node.get("summary").asText("") : content;
        } catch (Exception e) {
            return content;
        }
    }

    private AnalysisTask createTask(AnalysisType type) {
        return taskFactory.create(type);
    }

    private String buildConventionContent(GitHubApiService.PRUrl prUrl,
                                          List<String> candidates,
                                          GitHubApiService apiService) {
        if (candidates.isEmpty()) return "";

        // 顺序拉取（最多 3 个文件），避免向同一 analysisExecutor 提交子任务导致线程池死锁
        List<String> parts = new ArrayList<>();
        for (String name : candidates) {
            try {
                String content = apiService.fetchFileContent(prUrl, name).block();
                if (content != null && !content.isBlank()) {
                    parts.add("--- 来自 " + name + " ---\n" + content);
                }
            } catch (Exception e) {
                log.warn("Convention file fetch skipped: {} - {}", name, e.getMessage(), e);
            }
        }

        String combined = String.join("\n\n", parts);

        if (combined.isBlank()) return "";

        if (combined.length() > 8000) {
            combined = combined.substring(0, 8000);
            // 若截断位置落在 UTF-16 高代理字符上，移除孤立代理避免编码异常
            if (Character.isHighSurrogate(combined.charAt(combined.length() - 1))) {
                combined = combined.substring(0, combined.length() - 1);
            }
        }
        return "## 仓库约定（必须遵守）\n\n" + combined;
    }
}
