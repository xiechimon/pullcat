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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/**
 * 审查结果发布到 GitHub PR
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ReviewPublisherImpl implements ReviewPublisher {
    @Value("${pullcat.base-url:http://localhost:5173}")
    private String baseUrl;

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
        List<IssueRespDTO> dedupedIssues = deduplicateForPublish(resultAggregator.mergeResults(allResults));

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
        List<IssueRespDTO> dedupedIssues = deduplicateForPublish(resultAggregator.mergeResults(allResults));
        String summary = buildPublishSummary(dedupedIssues, session);
        apiService.publishReview(parsed, summary).block();
        session.setStatus(SessionStatus.PUBLISHED);
        reviewSessionService.save(session);
    }

    String buildPublishSummary(List<IssueRespDTO> dedupedIssues, ReviewSessionRespDTO session) {
        StringBuilder sb = new StringBuilder();
        sb.append("## 代码审查\n\n");

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

        sb.append("\n---\n");
        sb.append("### 审查详情\n\n");
        sb.append("- 审查记录：[").append(session.getId()).append("](").append(buildReviewDetailUrl(session)).append(")\n");
        sb.append("- PR 链接：").append(session.getPrUrl()).append("\n");
        sb.append("- 仓库：").append(session.getRepositoryFullName() != null ? session.getRepositoryFullName() : "-").append("\n\n");
        sb.append("*由 pullcat 自动生成*");
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

    private List<IssueRespDTO> deduplicateForPublish(List<IssueRespDTO> issues) {
        Map<String, IssueRespDTO> deduped = new LinkedHashMap<>();
        for (IssueRespDTO issue : issues) {
            String key = buildPublishDedupKey(issue);
            IssueRespDTO existing = deduped.get(key);
            if (existing == null) {
                deduped.put(key, issue);
                continue;
            }
            mergePublishIssue(existing, issue);
        }
        return new ArrayList<>(deduped.values());
    }

    private String buildPublishDedupKey(IssueRespDTO issue) {
        String file = safe(issue.getFile());
        String line = issue.getLine() != null ? String.valueOf(issue.getLine()) : "";
        String title = normalizeForPublish(issue.getTitle());
        String description = normalizeForPublish(issue.getDescription());
        String semantic = !title.isBlank() ? title : description;
        return file + ":" + line + ":" + semantic;
    }

    private void mergePublishIssue(IssueRespDTO existing, IssueRespDTO incoming) {
        if (incoming.getSeverity() != null
                && severityWeight(incoming) > severityWeight(existing)) {
            existing.setSeverity(incoming.getSeverity());
        }

        if (isLonger(incoming.getTitle(), existing.getTitle())) {
            existing.setTitle(incoming.getTitle());
        }

        if (isLonger(incoming.getDescription(), existing.getDescription())) {
            existing.setDescription(incoming.getDescription());
        }

        if (incoming.getConfidence() != null
                && (existing.getConfidence() == null || incoming.getConfidence() > existing.getConfidence())) {
            existing.setConfidence(incoming.getConfidence());
        }

        if (hasText(incoming.getSuggestion()) && !hasText(existing.getSuggestion())) {
            existing.setSuggestion(incoming.getSuggestion());
        }

        if (hasText(incoming.getSuggestionCode()) && !hasText(existing.getSuggestionCode())) {
            existing.setSuggestionCode(incoming.getSuggestionCode());
        }

        for (String dimension : incoming.getSourceDimensions()) {
            if (!existing.getSourceDimensions().contains(dimension)) {
                existing.getSourceDimensions().add(dimension);
            }
        }
    }

    private int severityWeight(IssueRespDTO issue) {
        return switch (Objects.requireNonNullElse(issue.getSeverity(), com.pullcat.common.enums.Severity.INFO)) {
            case CRITICAL -> 5;
            case HIGH -> 4;
            case MEDIUM -> 3;
            case LOW -> 2;
            case INFO -> 1;
        };
    }

    private boolean isLonger(String candidate, String current) {
        return hasText(candidate) && (!hasText(current) || candidate.length() > current.length());
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private String normalizeForPublish(String value) {
        if (!hasText(value)) {
            return "";
        }
        String normalized = Normalizer.normalize(value, Normalizer.Form.NFKC)
                .toLowerCase(Locale.ROOT)
                .replaceAll("\\s+", "")
                .replace("占位符", "占位")
                .replace("placeholder", "占位");
        return normalized.replaceAll("[\\p{Punct}，。！？、】【（）《》“”‘’·…—：；`~!@#$%^&*()_+\\-=\\[\\]{}\\\\|;:'\",.<>/?]+", "");
    }

    private String buildReviewDetailUrl(ReviewSessionRespDTO session) {
        String normalizedBaseUrl = hasText(baseUrl) ? baseUrl : "http://localhost:5173";
        if (normalizedBaseUrl.endsWith("/")) {
            normalizedBaseUrl = normalizedBaseUrl.substring(0, normalizedBaseUrl.length() - 1);
        }
        return normalizedBaseUrl + "/review/" + session.getId();
    }
}
