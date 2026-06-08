package com.pullcat.remote.dto.resp;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * GitHub 文件内容 API 响应 DTO，包含文件路径、内容和编码方式。
 */
@Data
@NoArgsConstructor
public class GitHubContentRespDTO {

    /**
     * 文件名
     */
    private String name;

    /**
     * 文件路径
     */
    private String path;

    /**
     * 文件内容（Base64 编码）
     */
    private String content;

    /**
     * 内容编码方式
     */
    private String encoding;

    /**
     * 文件类型（file/dir 等）
     */
    private String type;
}
