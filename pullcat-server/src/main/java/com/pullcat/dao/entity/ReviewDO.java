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
@TableName("review")
public class ReviewDO {

    @TableId(type = IdType.INPUT)
    private String id;

    @TableField("pr_url")
    private String prUrl;

    @TableField("repository_full_name")
    private String repositoryFullName;

    @TableField("user_id")
    private String userId;

    private String status;

    @TableField("published_comment_id")
    private Long publishedCommentId;

    @TableField("created_at")
    private Instant createdAt;

    @TableField("completed_at")
    private Instant completedAt;

    @TableField("updated_at")
    private Instant updatedAt;

    @TableField("snapshot_json")
    private String snapshotJson;
}
