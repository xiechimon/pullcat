package com.pullcat.dto.req;

import com.pullcat.common.enums.RuleType;
import com.pullcat.common.enums.Severity;
import lombok.Data;

/**
 * 规则新增或更新请求 DTO
 */
@Data
public class RuleUpsertReqDTO {

    /**
     * 规则类型
     */
    private RuleType type;

    /**
     * 匹配模式
     */
    private String pattern;

    /**
     * 严重级别
     */
    private Severity severity;

    /**
     * 规则名称
     */
    private String name;

    /**
     * 命中提示信息
     */
    private String message;

    /**
     * 修复建议
     */
    private String suggestion;

    /**
     * 是否启用
     */
    private boolean enabled = true;
}
