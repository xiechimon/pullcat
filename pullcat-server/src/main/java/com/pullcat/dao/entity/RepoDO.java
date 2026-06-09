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
@TableName("repo")
public class RepoDO {

    @TableField("owner")
    private String owner;

    @TableField("repo")
    private String repo;

    @TableId(value = "full_name", type = IdType.INPUT)
    private String fullName;

    private String description;

    private Integer stars;

    private String language;

    @TableField("added_at")
    private Instant addedAt = Instant.now();

    public RepoDO(String owner, String repo) {
        this.owner = owner;
        this.repo = repo;
        this.fullName = owner + "/" + repo;
    }
}
