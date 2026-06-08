package com.pullcat.remote.dto.resp;

import lombok.Data;

import java.util.List;

/**
 * GitHub 目录树响应 DTO。
 */
@Data
public class GitHubTreeRespDTO {

    /**
     * 树节点列表
     */
    private List<GitHubTreeNodeRespDTO> tree;
}
