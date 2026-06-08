package com.pullcat.dto.resp;

import lombok.Data;

/**
 * 仓库响应 DTO
 */
@Data
public class RepoRespDTO {

    /**
     * 仓库所属者
     */
    private String owner;

    /**
     * 仓库名
     */
    private String repo;

    /**
     * 仓库全名
     */
    private String fullName;

    /**
     * 仓库描述
     */
    private String description;

    /**
     * Star 数
     */
    private Integer stars;

    /**
     * 主语言
     */
    private String language;

    /**
     * 添加时间
     */
    private String addedAt;
}
