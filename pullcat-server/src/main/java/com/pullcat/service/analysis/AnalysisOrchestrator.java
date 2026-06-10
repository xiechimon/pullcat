package com.pullcat.service.analysis;

import com.pullcat.dto.resp.ReviewSessionRespDTO;

/**
 * 审查分析编排，负责异步调度多维分析任务并推送 SSE 事件
 */
public interface AnalysisOrchestrator {

    /**
     * 异步启动完整审查流程（抓取 PR 数据 → 并发执行分析 → 保存结果 → 自动发布）
     */
    void startAsync(ReviewSessionRespDTO session);
}
