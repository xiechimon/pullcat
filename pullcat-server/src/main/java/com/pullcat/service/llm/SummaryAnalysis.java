package com.pullcat.service.llm;

import com.pullcat.model.AnalysisType;
import org.springframework.ai.chat.client.ChatClient;

/**
 * 摘要分析任务，对 PR 变更进行概括性总结。
 */
public class SummaryAnalysis extends AnalysisTask {

    /**
     * 构造摘要分析任务。
     *
     * @param chatClient Spring AI ChatClient 实例
     * @param modelName  使用的模型名称
     */
    public SummaryAnalysis(ChatClient chatClient, String modelName) {
        super(chatClient, modelName, AnalysisType.SUMMARY);
    }

    @Override
    public String getTemplateName() {
        return "summary";
    }
}
