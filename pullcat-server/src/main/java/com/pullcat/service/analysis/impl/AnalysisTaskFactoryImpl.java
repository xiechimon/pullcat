package com.pullcat.service.analysis.impl;

import com.pullcat.common.enums.AnalysisType;
import com.pullcat.service.analysis.AnalysisTaskFactory;
import com.pullcat.service.llm.AnalysisTask;
import com.pullcat.service.llm.impl.ConsistencyAnalysisServiceImpl;
import com.pullcat.service.llm.impl.QualityAnalysisServiceImpl;
import com.pullcat.service.llm.impl.RiskAnalysisServiceImpl;
import com.pullcat.service.llm.impl.SummaryAnalysisServiceImpl;
import com.pullcat.service.llm.impl.TestingGapAnalysisServiceImpl;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * 分析任务工厂实现，持有 LLM 客户端与模型名称配置
 */
@Service
public class AnalysisTaskFactoryImpl implements AnalysisTaskFactory {

    private final ChatClient lightChatClient;
    private final ChatClient heavyChatClient;

    @Value("${pullcat.llm.light-model:deepseek-chat}")
    private String lightModelName;

    @Value("${pullcat.llm.heavy-model:deepseek-reasoner}")
    private String heavyModelName;

    public AnalysisTaskFactoryImpl(
            @Qualifier("lightChatClient") ChatClient lightChatClient,
            @Qualifier("heavyChatClient") ChatClient heavyChatClient) {
        this.lightChatClient = lightChatClient;
        this.heavyChatClient = heavyChatClient;
    }

    @Override
    public AnalysisTask create(AnalysisType type) {
        return switch (type) {
            case SUMMARY -> new SummaryAnalysisServiceImpl(lightChatClient, lightModelName);
            case RISK -> new RiskAnalysisServiceImpl(heavyChatClient, heavyModelName);
            case QUALITY -> new QualityAnalysisServiceImpl(heavyChatClient, heavyModelName);
            case CONSISTENCY -> new ConsistencyAnalysisServiceImpl(heavyChatClient, heavyModelName);
            case TESTING -> new TestingGapAnalysisServiceImpl(lightChatClient, lightModelName);
        };
    }
}
