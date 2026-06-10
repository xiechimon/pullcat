package com.pullcat.dto.resp;

import lombok.Data;

/**
 * 当前用户响应 DTO
 */
@Data
public class CurrentUserRespDTO {

    /**
     * 是否已认证
     */
    private boolean authenticated;

    /**
     * GitHub 登录名
     */
    private String login;

    /**
     * 头像链接
     */
    private String avatarUrl;

    /**
     * 展示名称
     */
    private String name;

    /**
     * 是否已安装 GitHub App
     */
    private boolean hasInstallation;
}
