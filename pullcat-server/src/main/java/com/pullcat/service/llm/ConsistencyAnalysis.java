package com.pullcat.service.llm;

import com.pullcat.model.AnalysisType;
import org.springframework.ai.chat.client.ChatClient;

/**
 * 代码一致性分析任务，检查 PR 变更与现有代码库风格是否一致。
 */
public class ConsistencyAnalysis extends AnalysisTask {

    /**
     * 构造代码一致性分析任务。
     *
     * @param chatClient Spring AI ChatClient 实例
     * @param modelName  使用的模型名称
     */
    public ConsistencyAnalysis(ChatClient chatClient, String modelName) {
        super(chatClient, modelName, AnalysisType.CONSISTENCY);
    }

    @Override
    public String getTemplateName() {
        return "consistency";
    }
}
