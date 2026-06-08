package com.pullcat.dto.resp;

import lombok.Data;

/**
 * SSE 规则建议事件响应 DTO
 */
@Data
public class SseRuleSuggestionRespDTO {

    /**
     * 提示消息
     */
    private String message;

    /**
     * 设置页地址
     */
    private String url;
}
