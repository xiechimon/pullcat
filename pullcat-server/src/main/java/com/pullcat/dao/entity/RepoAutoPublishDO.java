package com.pullcat.dao.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@TableName("repo_auto_publish")
public class RepoAutoPublishDO {

    @TableId(value = "full_name", type = IdType.INPUT)
    private String fullName;

    private String owner;

    private String repo;

    private boolean enabled;

    @TableField("created_at")
    private Instant createdAt = Instant.now();

    @TableField("updated_at")
    private Instant updatedAt = Instant.now();

    public RepoAutoPublishDO(String owner, String repo, boolean enabled) {
        this.fullName = owner + "/" + repo;
        this.owner = owner;
        this.repo = repo;
        this.enabled = enabled;
    }
}
