package com.pullcat.dto.resp;

import com.pullcat.common.enums.RuleType;
import com.pullcat.common.enums.Severity;
import lombok.Data;

/**
 * 规则响应 DTO
 */
@Data
public class RuleRespDTO {

    /**
     * 规则 ID
     */
    private String id;

    /**
     * 规则名称
     */
    private String name;

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
     * 提示消息
     */
    private String message;

    /**
     * 修复建议
     */
    private String suggestion;

    /**
     * 是否启用
     */
    private boolean enabled;
}
