package com.pullcat.remote.dto.resp;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * GitHub PR 文件信息响应 DTO，包含文件名、状态和增删改行数统计。
 */
@Data
@NoArgsConstructor
public class GitHubFileRespDTO {

    /**
     * 文件名（含路径）
     */
    private String filename;

    /**
     * 文件状态（added/modified/removed 等）
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
     * 文件原始内容的下载 URL
     */
    private String rawUrl;

    /**
     * GitHub Contents API 的 URL
     */
    private String contentsUrl;
}
