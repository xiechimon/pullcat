package com.pullcat.service.llm.impl;

import com.pullcat.common.enums.AnalysisType;
import com.pullcat.dto.resp.IssueRespDTO;
import com.pullcat.service.llm.AnalysisTask;
import org.springframework.ai.chat.client.ChatClient;

import java.util.List;

/**
 * 聚合汇总分析——将 5 维分析结果合并去重，生成可读摘要
 */
public class AggregationAnalysisServiceImpl extends AnalysisTask {

    public AggregationAnalysisServiceImpl(ChatClient chatClient, String modelName) {
        super(chatClient, modelName, AnalysisType.AGGREGATION);
    }

    @Override
    public String getTemplateName() {
        return "aggregation";
    }

    @Override
    protected List<IssueRespDTO> parseIssues(String response) {
        // 聚合任务输出纯 Markdown，不解析 issues
        return List.of();
    }
}
