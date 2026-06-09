package com.pullcat.service.llm.impl;

import com.pullcat.common.enums.AnalysisType;
import com.pullcat.service.llm.AnalysisTask;
import com.pullcat.service.llm.SummaryAnalysisService;
import org.springframework.ai.chat.client.ChatClient;

/**
 * 摘要分析服务，对 PR 变更进行概括性总结
 */
public class SummaryAnalysisServiceImpl extends AnalysisTask implements SummaryAnalysisService {

    public SummaryAnalysisServiceImpl(ChatClient chatClient, String modelName) {
        super(chatClient, modelName, AnalysisType.SUMMARY);
    }

    @Override
    public String getTemplateName() {
        return "summary";
    }
}
