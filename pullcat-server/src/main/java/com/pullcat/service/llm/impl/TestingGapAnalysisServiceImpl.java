package com.pullcat.service.llm.impl;

import com.pullcat.common.enums.AnalysisType;
import com.pullcat.service.llm.AnalysisTask;
import org.springframework.ai.chat.client.ChatClient;

/**
 * 测试覆盖分析服务，检查 PR 变更中的测试覆盖缺口
 */
public class TestingGapAnalysisServiceImpl extends AnalysisTask {

    public TestingGapAnalysisServiceImpl(ChatClient chatClient, String modelName) {
        super(chatClient, modelName, AnalysisType.TESTING);
    }

    @Override
    public String getTemplateName() {
        return "testing";
    }
}
