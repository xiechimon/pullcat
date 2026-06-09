package com.pullcat.service.llm.impl;

import com.pullcat.common.enums.AnalysisType;
import com.pullcat.service.llm.AnalysisTask;
import org.springframework.ai.chat.client.ChatClient;

/**
 * 风险分析服务，识别 PR 变更中的潜在风险点
 */
public class RiskAnalysisServiceImpl extends AnalysisTask {

    public RiskAnalysisServiceImpl(ChatClient chatClient, String modelName) {
        super(chatClient, modelName, AnalysisType.RISK);
    }

    @Override
    public String getTemplateName() {
        return "risk";
    }
}
