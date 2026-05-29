package com.pullcat.model;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 分析发现的问题，包含严重程度、文件位置、描述、建议和置信度。
 */
@Data
@NoArgsConstructor
public class Issue {

    /** 问题唯一标识 */
    private String id;
    /** 严重程度 */
    private Severity severity;
    /** 问题所在文件路径 */
    private String file;
    /** 问题所在行号 */
    private Integer line;
    /** 问题标题 */
    private String title;
    /** 问题详细描述 */
    private String description;
    /** 修改建议 */
    private String suggestion;
    /** AI 判断的置信度（0.0~1.0） */
    private Double confidence;
    /** 是否被用户选中用于发布 */
    private boolean selected = true;

    public Issue(String id, Severity severity, String file, Integer line, String title,
                 String description, String suggestion, Double confidence) {
        this.id = id;
        this.severity = severity;
        this.file = file;
        this.line = line;
        this.title = title;
        this.description = description;
        this.suggestion = suggestion;
        this.confidence = confidence;
        this.selected = true;
    }

    public enum Severity {
        CRITICAL, HIGH, MEDIUM, LOW, INFO
    }
}
