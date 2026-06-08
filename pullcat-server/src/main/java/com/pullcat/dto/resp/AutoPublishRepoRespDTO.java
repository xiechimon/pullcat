package com.pullcat.dto.resp;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 自动发布仓库响应 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AutoPublishRepoRespDTO {

    /**
     * 仓库所属组织或用户
     */
    private String owner;

    /**
     * 仓库名称
     */
    private String repo;

    /**
     * 是否启用自动发布
     */
    private boolean enabled;
}
