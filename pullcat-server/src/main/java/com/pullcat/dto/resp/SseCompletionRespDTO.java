package com.pullcat.dto.resp;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * SSE 完成事件响应 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SseCompletionRespDTO {

    /**
     * 完成状态
     */
    private String status;
}
