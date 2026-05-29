package com.pullcat.model;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * PR 元数据，包含标题、描述、仓库信息、分支及变更统计。
 */
@Data
@NoArgsConstructor
public class PRMetadata {

    /**
     * PR 标题
     */
    private String title;

    /**
     * PR 描述
     */
    private String description;

    /**
     * 仓库所有者
     */
    private String owner;

    /**
     * 仓库名称
     */
    private String repo;

    /**
     * PR 编号
     */
    private int pullNumber;

    /**
     * 目标分支
     */
    private String baseBranch;

    /**
     * 源分支
     */
    private String headBranch;

    /**
     * 变更文件数量
     */
    private int fileCount;

    /**
     * 新增行数
     */
    private int additions;

    /**
     * 删除行数
     */
    private int deletions;
}
