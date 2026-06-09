package com.pullcat.service.llm.impl;

import com.pullcat.common.enums.AnalysisType;
import com.pullcat.service.llm.AnalysisTask;
import com.pullcat.service.llm.QualityAnalysisService;
import org.springframework.ai.chat.client.ChatClient;

/**
 * 代码质量分析服务，评估 PR 变更中的代码质量
 */
public class QualityAnalysisServiceImpl extends AnalysisTask implements QualityAnalysisService {

    public QualityAnalysisServiceImpl(ChatClient chatClient, String modelName) {
        super(chatClient, modelName, AnalysisType.QUALITY);
    }

    @Override
    public String getTemplateName() {
        return "quality";
    }
}
