package com.pullcat.dto.resp;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 通用消息事件响应 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SseMessageRespDTO {

    /**
     * 消息内容
     */
    private String message;
}
