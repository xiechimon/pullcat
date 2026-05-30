package com.pullcat.model;

import lombok.Data;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 审查会话，记录单次 PR 审查的完整状态及各分析维度结果。
 */
@Data
public class ReviewSession {

    /** 会话唯一标识 */
    private String id;
    /** PR URL */
    private String prUrl;
    /** PR 元数据 */
    private PRMetadata prMetadata;
    /** 各分析类型的执行结果 */
    private Map<String, AnalysisResult> analyses = new LinkedHashMap<>();
    /** 当前会话状态 */
    private SessionStatus status;
    /** 创建时间 */
    private Instant createdAt = Instant.now();
    /** GitHub PR Review 评论 ID（发布后填充） */
    private Long publishedCommentId;
    /** 审查完成时间 */
    private Instant completedAt;
    /** 仓库全名，格式 owner/repo */
    private String repositoryFullName;
}
