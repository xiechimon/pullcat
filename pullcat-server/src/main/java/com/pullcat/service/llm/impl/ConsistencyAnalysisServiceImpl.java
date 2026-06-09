package com.pullcat.service.llm.impl;

import com.pullcat.common.enums.AnalysisType;
import com.pullcat.service.llm.AnalysisTask;
import org.springframework.ai.chat.client.ChatClient;

/**
 * 代码一致性分析服务，检查 PR 变更与现有代码库风格是否一致
 */
public class ConsistencyAnalysisServiceImpl extends AnalysisTask {

    public ConsistencyAnalysisServiceImpl(ChatClient chatClient, String modelName) {
        super(chatClient, modelName, AnalysisType.CONSISTENCY);
    }

    @Override
    public String getTemplateName() {
        return "consistency";
    }
}
