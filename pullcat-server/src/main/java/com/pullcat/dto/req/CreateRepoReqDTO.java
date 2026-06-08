package com.pullcat.dto.req;

import lombok.Data;

/**
 * 仓库新增请求 DTO
 */
@Data
public class CreateRepoReqDTO {

    /**
     * 仓库所属组织或用户
     */
    private String owner;

    /**
     * 仓库名称
     */
    private String repo;

    /**
     * 仓库描述
     */
    private String description;
}
