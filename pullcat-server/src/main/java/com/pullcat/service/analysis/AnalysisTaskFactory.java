package com.pullcat.service.analysis;

import com.pullcat.common.enums.AnalysisType;
import com.pullcat.service.llm.AnalysisTask;

/**
 * 分析任务工厂，根据分析类型创建对应任务实例
 */
public interface AnalysisTaskFactory {

    /**
     * 根据分析类型创建任务实例
     */
    AnalysisTask create(AnalysisType type);
}
