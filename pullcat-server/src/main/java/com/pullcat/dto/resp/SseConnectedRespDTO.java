package com.pullcat.dto.resp;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * SSE 连接建立事件响应 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SseConnectedRespDTO {

    /**
     * 审查 ID
     */
    private String reviewId;
}
