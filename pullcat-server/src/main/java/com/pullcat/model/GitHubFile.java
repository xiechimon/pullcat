package com.pullcat.model;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * GitHub PR 文件信息，包含文件名、状态和增删改行数统计。
 */
@Data
@NoArgsConstructor
public class GitHubFile {
    /**
     * 文件名（含路径）
     */
    private String filename;

    /**
     * 文件状态（added/modified/removed等）
     */
    private String status;

    /**
     * 新增行数
     */
    private int additions;

    /**
     * 删除行数
     */
    private int deletions;

    /**
     * 变更行数
     */
    private int changes;

    /**
     * 文件原始内容的下载URL
     */
    private String rawUrl;

    /**
     * GitHub Contents API的URL
     */
    private String contentsUrl;
}
