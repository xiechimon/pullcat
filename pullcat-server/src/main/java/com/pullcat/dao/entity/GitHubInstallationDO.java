package com.pullcat.dao.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * GitHub App 安装记录
 */
@Data
@NoArgsConstructor
@TableName("github_installation")
public class GitHubInstallationDO {

    /**
     * GitHub App Installation ID
     */
    @TableId(value = "installation_id", type = IdType.INPUT)
    private Long installationId;

    /**
     * GitHub 用户名或 Org 名，与 UserDO.githubLogin 语义一致
     */
    @TableField("account_login")
    private String accountLogin;

    /**
     * 账号类型，'User' 或 'Organization'
     */
    @TableField("account_type")
    private String accountType;

    /**
     * 安装时间
     */
    @TableField("installed_at")
    private Instant installedAt;

    /**
     * 暂停/卸载时间，NULL 表示活跃
     */
    @TableField("suspended_at")
    private Instant suspendedAt;
}
