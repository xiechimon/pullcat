package com.pullcat.dto.req;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * GitHub Webhook 中的 Pull Request 请求 DTO
 */
@Data
public class WebhookPullRequestReqDTO {

    /**
     * Pull Request 页面链接
     */
    @JsonProperty("html_url")
    private String htmlUrl;

    private HeadReqDTO head;

    @Data
    public static class HeadReqDTO {
        private String sha;
    }
}
