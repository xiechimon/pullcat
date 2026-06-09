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

@Data
@NoArgsConstructor
@TableName("rule")
public class RuleDO {

    @TableId(type = IdType.INPUT)
    private String id;

    @TableField("repo_owner")
    private String repoOwner;

    @TableField("repo_name")
    private String repoName;

    private RuleType type;

    private String pattern;

    private Severity severity;

    private String name;

    private String message;

    private String suggestion;

    private boolean enabled = true;

    @TableField("created_at")
    private Instant createdAt = Instant.now();
}
