package com.pullcat.service.analysis.impl;

import com.pullcat.common.enums.AnalysisType;
import com.pullcat.service.analysis.AnalysisTaskFactory;
import com.pullcat.service.llm.AnalysisTask;
import com.pullcat.service.llm.impl.ConsistencyAnalysisServiceImpl;
import com.pullcat.service.llm.impl.QualityAnalysisServiceImpl;
import com.pullcat.service.llm.impl.RiskAnalysisServiceImpl;
import com.pullcat.service.llm.impl.SummaryAnalysisServiceImpl;
import com.pullcat.service.llm.impl.TestingGapAnalysisServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * 分析任务工厂实现，持有 LLM 客户端与模型名称配置
 */
@Service
@RequiredArgsConstructor
public class AnalysisTaskFactoryImpl implements AnalysisTaskFactory {

    @Qualifier("lightChatClient")
    private final ChatClient lightChatClient;
    @Qualifier("heavyChatClient")
    private final ChatClient heavyChatClient;

    @Value("${pullcat.llm.light-model:deepseek-chat}")
    private String lightModelName;

    @Value("${pullcat.llm.heavy-model:deepseek-reasoner}")
    private String heavyModelName;

    @Override
    public AnalysisTask create(AnalysisType type) {
        return switch (type) {
            case SUMMARY -> new SummaryAnalysisServiceImpl(lightChatClient, lightModelName);
            case RISK -> new RiskAnalysisServiceImpl(heavyChatClient, heavyModelName);
            case QUALITY -> new QualityAnalysisServiceImpl(heavyChatClient, heavyModelName);
            case CONSISTENCY -> new ConsistencyAnalysisServiceImpl(heavyChatClient, heavyModelName);
            case TESTING -> new TestingGapAnalysisServiceImpl(lightChatClient, lightModelName);
            case AGGREGATION -> throw new UnsupportedOperationException("AGGREGATION analysis not yet implemented");
        };
    }
}
