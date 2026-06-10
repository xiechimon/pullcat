package com.pullcat.service.analysis.impl;

import com.pullcat.common.enums.SessionStatus;
import com.pullcat.dto.resp.AnalysisResultRespDTO;
import com.pullcat.dto.resp.IssueRespDTO;
import com.pullcat.dto.resp.ReviewSessionRespDTO;
import com.pullcat.remote.GitHubApiService;
import com.pullcat.service.AutoPublishService;
import com.pullcat.service.analysis.ResultAggregator;
import com.pullcat.service.analysis.ReviewPublisher;
import com.pullcat.service.analysis.ReviewSessionService;
import com.pullcat.toolkit.MarkdownUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 审查结果发布到 GitHub PR
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ReviewPublisherImpl implements ReviewPublisher {

    private final GitHubApiService gitHubApiService;
    private final ReviewSessionService reviewSessionService;
    private final ResultAggregator resultAggregator;
    private final AutoPublishService autoPublishService;

    /**
     * 发布审查结果到 PR，支持选择性发布和摘要配置
     */
    @Override
    public ReviewSessionRespDTO publishReview(String reviewId) {
        ReviewSessionRespDTO session = reviewSessionService.findById(reviewId);
        if (session == null) {
            throw new IllegalArgumentException("Review session not found: " + reviewId);
        }

        GitHubApiService apiService = resolveGitHubApiService(session);
        GitHubApiService.PRUrl parsed = apiService.parsePrUrl(session.getPrUrl());

        List<AnalysisResultRespDTO> allResults = new ArrayList<>(session.getAnalyses().values());
        List<IssueRespDTO> dedupedIssues = resultAggregator.mergeResults(allResults);

        String summary = buildPublishSummary(dedupedIssues, session);

        List<GitHubApiService.ReviewComment> comments = dedupedIssues.stream()
                .filter(issue -> issue.getSuggestionCode() != null && !issue.getSuggestionCode().isBlank())
                .filter(issue -> issue.getFile() != null && issue.getLine() != null)
                .map(issue -> new GitHubApiService.ReviewComment(
                        issue.getFile(), issue.getLine(), MarkdownUtil.buildSuggestionBlock(issue)))
                .toList();

        Long commentId = apiService.publishReviewWithComments(parsed, summary, comments).block();
        session.setStatus(SessionStatus.PUBLISHED);
        session.setPublishedCommentId(commentId);
        reviewSessionService.save(session);

        return session;
    }

    @Override
    public boolean tryAutoPublish(ReviewSessionRespDTO session) {
        String fullName = session.getRepositoryFullName();
        if (fullName == null) {
            return false;
        }
        String[] parts = fullName.split("/", 2);
        if (parts.length != 2) {
            return false;
        }

        if (autoPublishService.getStatus(parts[0], parts[1]).isEnabled()) {
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

    String buildPublishSummary(List<IssueRespDTO> dedupedIssues, ReviewSessionRespDTO session) {
        StringBuilder sb = new StringBuilder();
        sb.append("## AI 代码审查\n\n");

        AnalysisResultRespDTO summaryResult = session.getAnalyses().get("summary");
        if (summaryResult != null && summaryResult.getContent() != null) {
            sb.append("### 审查摘要\n\n").append(MarkdownUtil.extractSummaryText(summaryResult.getContent())).append("\n\n");
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

    @Override
    public String buildConventionContent(GitHubApiService.PRUrl prUrl, List<String> candidates) {
        if (candidates.isEmpty()) return "";

        List<String> parts = new ArrayList<>();
        for (String name : candidates) {
            try {
                String content = gitHubApiService.fetchFileContent(prUrl, name).block();
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
            if (Character.isHighSurrogate(combined.charAt(combined.length() - 1))) {
                combined = combined.substring(0, combined.length() - 1);
            }
        }
        return "## 仓库约定（必须遵守）\n\n" + combined;
    }

    private GitHubApiService resolveGitHubApiService(ReviewSessionRespDTO session) {
        if (session.getInstallationId() == null) {
            return gitHubApiService;
        }
        return gitHubApiService.withInstallationToken(session.getInstallationId()).blockOptional()
                .orElse(gitHubApiService);
    }
}
