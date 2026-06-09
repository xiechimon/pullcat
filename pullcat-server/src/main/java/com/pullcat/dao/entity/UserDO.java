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
@TableName("user")
public class UserDO {

    @TableId(type = IdType.INPUT)
    private String id;

    @TableField("github_login")
    private String githubLogin;

    @TableField("github_id")
    private Long githubId;

    @TableField("avatar_url")
    private String avatarUrl;

    private String email;

    @TableField("created_at")
    private Instant createdAt = Instant.now();
}
