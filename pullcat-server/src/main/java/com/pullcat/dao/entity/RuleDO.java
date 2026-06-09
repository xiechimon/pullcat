package com.pullcat.dao.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.pullcat.common.enums.RuleType;
import com.pullcat.common.enums.Severity;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * 仓库规则
 */
@Data
@NoArgsConstructor
@TableName("rule")
public class RuleDO {

    /**
     * 规则 ID
     */
    @TableId(type = IdType.INPUT)
    private String id;

    /**
     * 仓库 owner
     */
    @TableField("repo_owner")
    private String repoOwner;

    /**
     * 仓库名
     */
    @TableField("repo_name")
    private String repoName;

    /**
     * 规则类型枚举
     */
    private RuleType type;

    /**
     * 匹配模式
     */
    private String pattern;

    /**
     * 严重级别枚举
     */
    private Severity severity;

    /**
     * 规则名称
     */
    private String name;

    /**
     * 问题提示
     */
    private String message;

    /**
     * 修复建议
     */
    private String suggestion;

    /**
     *
     * 是否启用
     */
    private boolean enabled = true;

    /**
     * 创建时间
     */
    @TableField("created_at")
    private Instant createdAt = Instant.now();
}
