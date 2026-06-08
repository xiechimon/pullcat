package com.pullcat.dto.resp;

import lombok.Data;

/**
 * Webhook 处理响应 DTO
 */
@Data
public class WebhookRespDTO {

    /**
     * 处理状态
     */
    private String status;

    /**
     * 忽略原因
     */
    private String reason;

    /**
     * 事件动作
     */
    private String action;

    /**
     * Pull Request 链接
     */
    private String prUrl;
}
