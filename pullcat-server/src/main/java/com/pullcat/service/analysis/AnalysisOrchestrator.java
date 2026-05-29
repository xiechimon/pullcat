package com.pullcat.service.analysis;

import com.pullcat.model.*;
import com.pullcat.service.github.GitHubApiService;
import com.pullcat.service.llm.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;

/**
 * 分析编排器，负责完整的 PR 审查流程：拉取数据、构建上下文、执行五个分析任务。
 */
@Slf4j
@Service
public class AnalysisOrchestrator {

    private final GitHubApiService gitHubApiService;
    private final PromptLoader promptLoader;
    private final ContextBuilder contextBuilder;
    private final ResultAggregator resultAggregator;
    private final ChatClient chatClient;

    public AnalysisOrchestrator(GitHubApiService gitHubApiService,
                                PromptLoader promptLoader,
                                ContextBuilder contextBuilder,
                                ResultAggregator resultAggregator,
                                ChatClient chatClient) {
        this.gitHubApiService = gitHubApiService;
        this.promptLoader = promptLoader;
        this.contextBuilder = contextBuilder;
        this.resultAggregator = resultAggregator;
        this.chatClient = chatClient;
    }

    /**
     * 执行完整的 PR 审查流程，返回包含所有分析结果的 ReviewSession。
     */
    public ReviewSession startReview(String prUrl) {
        GitHubApiService.PRUrl parsed = gitHubApiService.parsePrUrl(prUrl);

        ReviewSession session = new ReviewSession();
        session.setId(UUID.randomUUID().toString());
        session.setPrUrl(prUrl);

        PRData prData = gitHubApiService.fetchPRData(parsed).block();
        session.setPrMetadata(prData.getMetadata());

        Map<String, String> variables = contextBuilder.buildVariables(
                prData.getMetadata(), prData.getFileTree(), prData.getFiles());

        session.getAnalyses().put("summary", executeTask(AnalysisType.SUMMARY, variables));
        session.getAnalyses().put("risk", executeTask(AnalysisType.RISK, variables));
        session.getAnalyses().put("quality", executeTask(AnalysisType.QUALITY, variables));
        session.getAnalyses().put("consistency", executeTask(AnalysisType.CONSISTENCY, variables));
        session.getAnalyses().put("testing", executeTask(AnalysisType.TESTING, variables));

        return session;
    }

    private AnalysisResult executeTask(AnalysisType type, Map<String, String> variables) {
        AnalysisTask task = createTask(type);
        String template = promptLoader.loadTemplate(type.getTemplateName());
        String prompt = promptLoader.populateTemplate(template, variables);
        return task.execute(prompt).block();
    }

    private AnalysisTask createTask(AnalysisType type) {
        String modelName = "deepseek-chat";
        switch (type) {
            case SUMMARY: return new SummaryAnalysis(chatClient, modelName);
            case RISK: return new RiskAnalysis(chatClient, modelName);
            case QUALITY: return new QualityAnalysis(chatClient, modelName);
            case CONSISTENCY: return new ConsistencyAnalysis(chatClient, modelName);
            case TESTING: return new TestingGapAnalysis(chatClient, modelName);
            default: throw new IllegalArgumentException("Unknown analysis type: " + type);
        }
    }
}
