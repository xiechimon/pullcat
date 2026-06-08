package com.pullcat.dto.resp;

import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 全局统计概览响应 DTO
 */
@Data
public class StatsOverviewRespDTO {

    /**
     * 审查总数
     */
    private int totalReviews;

    /**
     * 问题总数
     */
    private int totalIssues;

    /**
     * 仓库总数
     */
    private int repoCount;

    /**
     * 平均每次审查问题数
     */
    private double avgIssuesPerReview;

    /**
     * 严重级别分布
     */
    private Map<String, Integer> severityDistribution;

    /**
     * 高频问题类型
     */
    private List<CommonIssueTypeRespDTO> commonIssueTypes;
}
