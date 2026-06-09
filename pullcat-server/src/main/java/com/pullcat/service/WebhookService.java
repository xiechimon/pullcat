package com.pullcat.service;

import com.pullcat.dto.req.WebhookEventReqDTO;
import com.pullcat.dto.resp.WebhookRespDTO;

/**
 * Webhook 业务服务
 */
public interface WebhookService {

    /**
     * 处理 GitHub Webhook 事件，返回处理结果
     */
    WebhookRespDTO handle(String eventType, WebhookEventReqDTO requestParam);
}
