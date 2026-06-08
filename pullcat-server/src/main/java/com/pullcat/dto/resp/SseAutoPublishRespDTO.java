package com.pullcat.dto.resp;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * SSE 自动发布事件响应 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SseAutoPublishRespDTO {

    /**
     * PR 地址
     */
    private String prUrl;
}
