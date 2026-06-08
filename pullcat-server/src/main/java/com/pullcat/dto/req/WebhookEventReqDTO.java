package com.pullcat.dto.req;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * GitHub Webhook 事件请求 DTO
 */
@Data
public class WebhookEventReqDTO {

    /**
     * 事件动作
     */
    private String action;

    /**
     * Pull Request 数据
     */
    @JsonProperty("pull_request")
    private WebhookPullRequestReqDTO pullRequest;
}
