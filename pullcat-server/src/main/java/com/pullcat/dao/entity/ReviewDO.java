package com.pullcat.dao.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * 审查会话
 */
@Data
@NoArgsConstructor
@TableName("review")
public class ReviewDO {

    /**
     * 审查 ID
     */
    @TableId(type = IdType.INPUT)
    private String id;

    /**
     * PR 地址
     */
    @TableField("pr_url")
    private String prUrl;

    /**
     * 仓库全名 owner/repo
     */
    @TableField("repository_full_name")
    private String repositoryFullName;

    /**
     * 创建用户登录名
     */
    @TableField("user_id")
    private String userId;

    /**
     * 会话状态
     */
    private String status;

    /**
     * 已发布评论 ID
     */
    @TableField("published_comment_id")
    private Long publishedCommentId;

    /**
     * 创建时间
     */
    @TableField("created_at")
    private Instant createdAt;

    /**
     * 完成时间
     */
    @TableField("completed_at")
    private Instant completedAt;

    /**
     * 更新时间
     */
    @TableField("updated_at")
    private Instant updatedAt;

    /**
     * 完整会话快照 JSON
     */
    @TableField("snapshot_json")
    private String snapshotJson;
}
