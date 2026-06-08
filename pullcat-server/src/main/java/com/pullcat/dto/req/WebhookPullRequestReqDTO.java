package com.pullcat.dto.req;

import lombok.Data;

/**
 * GitHub Webhook 中的 Pull Request 请求 DTO
 */
@Data
public class WebhookPullRequestReqDTO {

    /**
     * Pull Request 页面链接
     */
    private String htmlUrl;
}
