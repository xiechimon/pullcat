package com.pullcat.dto.resp;

import lombok.Data;

import java.util.Map;

/**
 * 仓库统计响应 DTO
 */
@Data
public class RepoStatsRespDTO {

    /**
     * 审查总数
     */
    private int totalReviews;

    /**
     * 问题总数
     */
    private int totalIssues;

    /**
     * 平均每次审查问题数
     */
    private double avgIssuesPerReview;

    /**
     * 严重级别分布
     */
    private Map<String, Integer> severityDistribution;

    /**
     * 仓库全名
     */
    private String repoFullName;
}
