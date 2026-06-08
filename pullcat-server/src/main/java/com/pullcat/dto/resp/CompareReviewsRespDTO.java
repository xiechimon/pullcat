package com.pullcat.dto.resp;

import lombok.Data;

/**
 * 审查结果对比响应 DTO
 */
@Data
public class CompareReviewsRespDTO {

    /**
     * 第一份审查引用
     */
    private ReviewRefRespDTO review1;

    /**
     * 第二份审查引用
     */
    private ReviewRefRespDTO review2;

    /**
     * 新增问题数
     */
    private int newCount;

    /**
     * 已修复问题数
     */
    private int fixedCount;

    /**
     * 持续存在的问题数
     */
    private int persistentCount;

    /**
     * 第一份审查问题总数
     */
    private int totalIssues1;

    /**
     * 第二份审查问题总数
     */
    private int totalIssues2;
}
