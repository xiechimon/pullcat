package com.pullcat.service.llm;

import com.pullcat.model.AnalysisType;
import org.springframework.ai.chat.client.ChatClient;

/**
 * 测试覆盖分析任务，检查 PR 变更中的测试覆盖缺口。
 */
public class TestingGapAnalysis extends AnalysisTask {

    /**
     * 构造测试覆盖分析任务。
     *
     * @param chatClient Spring AI ChatClient 实例
     * @param modelName  使用的模型名称
     */
    public TestingGapAnalysis(ChatClient chatClient, String modelName) {
        super(chatClient, modelName, AnalysisType.TESTING);
    }

    @Override
    public String getTemplateName() {
        return "testing";
    }
}
