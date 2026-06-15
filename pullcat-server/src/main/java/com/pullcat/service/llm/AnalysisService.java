package com.pullcat.service.llm;

import com.pullcat.common.enums.AnalysisType;
import com.pullcat.dto.resp.AnalysisResultRespDTO;
import reactor.core.publisher.Mono;

/**
 * 五维分析统一接口
 * 将 prompt 发送给 LLM 并获取分析结果
 * 告知 PromptLoader 该维度对应的模板文件
 * 标识当前维度类型（summary / risk / quality / consistency / testing / aggregation）
 * 在任务执行过程中或执行完成后获取实时状态与结果
 */
public interface AnalysisService {

    /**
     * 执行 LLM 分析任务
     *
     * @param prompt 发送给 LLM 的完整提示词
     * @return 包含分析结果的响应式
     */
    Mono<AnalysisResultRespDTO> execute(String prompt);

    /**
     * 获取本分析维度对应的提示词模板名称
     *
     * @return 模板名称
     */
    String getTemplateName();

    /**
     * 获取本分析任务的分析类型枚举。
     *
     * @return 分析类型枚举值，不会为 null
     */
    AnalysisType getType();

    /**
     * 获取当前分析任务的结果对象
     *
     * @return 分析结果对象，包含类型、状态、模型名称、原始内容、问题列表、Token 用量、时间戳和错误信息
     */
    AnalysisResultRespDTO getResult();
}
