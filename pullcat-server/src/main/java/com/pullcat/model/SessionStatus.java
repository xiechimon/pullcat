package com.pullcat.model;

/**
 * 审查会话状态枚举，表示一次 PR 审查会话的当前阶段。
 */
public enum SessionStatus {

    /** 获取 PR 数据中 */
    FETCHING,

    /** 分析进行中 */
    ANALYZING,

    /** 审查完成 */
    COMPLETED,

    /** 审查失败 */
    FAILED,

    /** 已发布评论 */
    PUBLISHED
}
