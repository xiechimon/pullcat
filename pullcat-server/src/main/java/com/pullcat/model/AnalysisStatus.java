package com.pullcat.model;

/**
 * 分析状态枚举，表示分析任务的当前生命周期状态。
 */
public enum AnalysisStatus {

    /** 等待执行 */
    PENDING,

    /** 正在执行 */
    RUNNING,

    /** 执行完成 */
    COMPLETED,

    /** 执行失败 */
    FAILED
}
