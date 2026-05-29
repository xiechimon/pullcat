package com.pullcat.service.llm;

import com.pullcat.model.AnalysisType;
import org.springframework.ai.chat.client.ChatClient;

/**
 * 代码质量分析任务，评估 PR 变更中的代码质量。
 */
public class QualityAnalysis extends AnalysisTask {

    /**
     * 构造代码质量分析任务。
     *
     * @param chatClient Spring AI ChatClient 实例
     * @param modelName  使用的模型名称
     */
    public QualityAnalysis(ChatClient chatClient, String modelName) {
        super(chatClient, modelName, AnalysisType.QUALITY);
    }

    @Override
    public String getTemplateName() {
        return "quality";
    }
}
