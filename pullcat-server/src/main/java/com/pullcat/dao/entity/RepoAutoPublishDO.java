package com.pullcat.dao.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * 仓库自动发布配置
 */
@Data
@NoArgsConstructor
@TableName("repo_auto_publish")
public class RepoAutoPublishDO {

    /**
     * owner/repo
     */
    @TableId(value = "full_name", type = IdType.INPUT)
    private String fullName;

    /**
     * 仓库 owner
     */
    private String owner;

    /**
     * 仓库名
     */
    private String repo;

    /**
     * 是否启用自动发布
     */
    private boolean enabled;

    /**
     * 创建时间
     */
    @TableField("created_at")
    private Instant createdAt = Instant.now();

    /**
     * 更新时间
     */
    @TableField("updated_at")
    private Instant updatedAt = Instant.now();

    public RepoAutoPublishDO(String owner, String repo, boolean enabled) {
        this.fullName = owner + "/" + repo;
        this.owner = owner;
        this.repo = repo;
        this.enabled = enabled;
    }
}
