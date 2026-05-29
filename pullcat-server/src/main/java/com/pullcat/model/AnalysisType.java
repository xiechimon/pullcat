package com.pullcat.model;

/**
 * 分析类型枚举，定义系统支持的分析种类及其对应的提示词模板名称。
 */
public enum AnalysisType {

    /** 摘要分析 */
    SUMMARY("summary"),

    /** 风险分析 */
    RISK("risk"),

    /** 代码质量分析 */
    QUALITY("quality"),

    /** 代码一致性分析 */
    CONSISTENCY("consistency"),

    /** 测试覆盖率分析 */
    TESTING("testing");

    private final String templateName;

    AnalysisType(String templateName) {
        this.templateName = templateName;
    }

    /**
     * 获取该分析类型对应的提示词模板名称
     *
     * @return 模板名称
     */
    public String getTemplateName() {
        return templateName;
    }
}
