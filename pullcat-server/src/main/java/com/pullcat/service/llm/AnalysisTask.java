package com.pullcat.service.llm;

import com.pullcat.config.RetryConfig;
import com.pullcat.model.AnalysisResult;
import com.pullcat.model.AnalysisStatus;
import com.pullcat.model.AnalysisType;
import com.pullcat.model.Issue;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.List;

@Slf4j
public abstract class AnalysisTask {

    protected final ChatClient chatClient;

    protected final String modelName;

    protected final AnalysisType analysisType;

    protected final AnalysisResult result;

    /**
     * 构造分析任务，初始化 ChatClient、模型名称及分析类型，并创建对应的结果对象。
     *
     * @param chatClient   Spring AI ChatClient 实例
     * @param modelName    使用的模型名称
     * @param analysisType 分析类型
     */
    protected AnalysisTask(ChatClient chatClient, String modelName, AnalysisType analysisType) {
        this.chatClient = chatClient;
        this.modelName = modelName;
        this.analysisType = analysisType;
        this.result = new AnalysisResult(analysisType);
        this.result.setModel(modelName);
    }

    /**
     * 执行分析任务：调用 LLM 获取响应，解析问题列表，填充结果。
     *
     * @param prompt 发送给 LLM 的提示词
     * @return 包含分析结果的 Mono
     */
    public Mono<AnalysisResult> execute(String prompt) {
        return Mono.fromCallable(() -> {
            result.setStatus(AnalysisStatus.RUNNING);
            result.setStartedAt(Instant.now());

            String response = chatClient.prompt()
                    .user(prompt)
                    .call()
                    .content();

            result.setContent(response);
            List<Issue> issues = parseIssues(response);
            assignIssueIds(issues);
            result.setIssues(issues);
            result.setStatus(AnalysisStatus.COMPLETED);
            result.setCompletedAt(Instant.now());

            return result;
        }).retryWhen(RetryConfig.llmRetry())
          .onErrorResume(e -> {
            log.warn("Analysis task {} failed after retries: {}", analysisType, e.getMessage());
            result.setStatus(AnalysisStatus.FAILED);
            result.setErrorMessage(e.getMessage());
            result.setCompletedAt(Instant.now());
            return Mono.just(result);
        });
    }

    /**
     * 解析 LLM 响应文本中的问题列表。
     *
     * @param response LLM 返回的原始文本
     * @return 解析后的问题列表
     */
    protected List<Issue> parseIssues(String response) {
        return JsonOutputParser.parseIssues(response);
    }

    /**
     * 为问题列表中的每个问题分配唯一标识。
     *
     * @param issues 问题列表
     */
    private void assignIssueIds(List<Issue> issues) {
        for (int i = 0; i < issues.size(); i++) {
            issues.get(i).setId(analysisType.name() + "-" + (i + 1));
        }
    }

    /**
     * 获取当前分析任务对应的提示词模板名称。
     *
     * @return 模板名称
     */
    public abstract String getTemplateName();

    /**
     * 获取当前分析任务的分析类型。
     *
     * @return 分析类型
     */
    public AnalysisType getType() {
        return analysisType;
    }

    /**
     * 获取当前分析任务的结果对象。
     *
     * @return 分析结果
     */
    public AnalysisResult getResult() {
        return result;
    }
}
