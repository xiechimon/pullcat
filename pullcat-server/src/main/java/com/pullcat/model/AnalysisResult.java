package com.pullcat.model;

import lombok.Data;

import java.time.Instant;
import java.util.List;

/**
 * 分析结果模型，封装单次分析的完整信息，包括类型、状态、输出内容及耗时等。
 */
@Data
public class AnalysisResult {

    /** 分析类型 */
    private AnalysisType type;

    /** 分析状态，默认为待执行 */
    private AnalysisStatus status = AnalysisStatus.PENDING;

    /** 使用的 AI 模型名称 */
    private String model;

    /** 分析输出的文本内容 */
    private String content;

    /** 分析发现的问题列表 */
    private List<Issue> issues;

    /** Token 消耗量 */
    private int tokensUsed;

    /** 分析开始时间 */
    private Instant startedAt;

    /** 分析完成时间 */
    private Instant completedAt;

    /** 分析失败时的错误信息 */
    private String errorMessage;

    /**
     * 无参构造函数，status 默认为 PENDING。
     */
    public AnalysisResult() {
        this.status = AnalysisStatus.PENDING;
    }

    /**
     * 根据分析类型创建结果实例，status 默认为 PENDING。
     *
     * @param type 分析类型
     */
    public AnalysisResult(AnalysisType type) {
        this.type = type;
        this.status = AnalysisStatus.PENDING;
    }
}
