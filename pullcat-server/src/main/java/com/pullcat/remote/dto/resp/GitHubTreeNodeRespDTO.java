package com.pullcat.remote.dto.resp;

import lombok.Data;

/**
 * GitHub 树节点响应 DTO。
 */
@Data
public class GitHubTreeNodeRespDTO {

    /**
     * 节点路径
     */
    private String path;

    /**
     * 节点类型
     */
    private String type;
}
