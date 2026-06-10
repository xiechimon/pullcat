package com.pullcat.dao.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * 用户信息
 */
@Data
@NoArgsConstructor
@TableName("user")
public class UserDO {

    /**
     * 用户 ID
     */
    @TableId(type = IdType.INPUT)
    private String id;

    /**
     * GitHub 登录名
     */
    @TableField("github_login")
    private String githubLogin;

    /**
     * GitHub 数字 ID
     */
    @TableField("github_id")
    private Long githubId;

    /**
     * 头像地址
     */
    @TableField("avatar_url")
    private String avatarUrl;

    /**
     * 邮箱
     */
    private String email;

    /**
     * 创建时间
     */
    @TableField("created_at")
    private Instant createdAt = Instant.now();

    /**
     * GitHub App Installation ID
     */
    @TableField("installation_id")
    private Long installationId;
}
