package com.pullcat.remote.dto.req;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * GitHub commit status 请求 DTO。
 */
@Data
public class GitHubCommitStatusReqDTO {

    /**
     * 状态值
     */
    private String state;

    /**
     * 状态描述
     */
    private String description;

    /**
     * 状态上下文
     */
    private String context;

    /**
     * 状态详情页 URL
     */
    @JsonProperty("target_url")
    private String targetUrl;
}
