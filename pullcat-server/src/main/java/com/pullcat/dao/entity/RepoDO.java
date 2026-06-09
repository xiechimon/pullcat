package com.pullcat.dao.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * 仓库信息
 */
@Data
@NoArgsConstructor
@TableName("repo")
public class RepoDO {

    /**
     * 仓库 owner
     */
    @TableField("owner")
    private String owner;

    /**
     * 仓库名
     */
    @TableField("repo")
    private String repo;

    /**
     * owner/repo
     */
    @TableId(value = "full_name", type = IdType.INPUT)
    private String fullName;

    /**
     * 仓库描述
     */
    private String description;

    /**
     * Star 数
     */
    private Integer stars;

    /**
     * 主语言
     */
    private String language;

    /**
     * 添加时间
     */
    @TableField("added_at")
    private Instant addedAt = Instant.now();

    public RepoDO(String owner, String repo) {
        this.owner = owner;
        this.repo = repo;
        this.fullName = owner + "/" + repo;
    }
}
