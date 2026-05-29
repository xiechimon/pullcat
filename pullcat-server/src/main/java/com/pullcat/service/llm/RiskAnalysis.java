package com.pullcat.service.llm;

import com.pullcat.model.AnalysisType;
import org.springframework.ai.chat.client.ChatClient;

/**
 * 风险分析任务，识别 PR 变更中的潜在风险点。
 */
public class RiskAnalysis extends AnalysisTask {

    /**
     * 构造风险分析任务。
     *
     * @param chatClient Spring AI ChatClient 实例
     * @param modelName  使用的模型名称
     */
    public RiskAnalysis(ChatClient chatClient, String modelName) {
        super(chatClient, modelName, AnalysisType.RISK);
    }

    @Override
    public String getTemplateName() {
        return "risk";
    }
}
