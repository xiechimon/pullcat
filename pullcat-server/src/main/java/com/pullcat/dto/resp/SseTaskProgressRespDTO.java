package com.pullcat.dto.resp;

import lombok.Data;

/**
 * SSE 任务进度事件响应 DTO
 */
@Data
public class SseTaskProgressRespDTO {

    /**
     * 任务名
     */
    private String task;

    /**
     * 当前状态
     */
    private String status;

    /**
     * 模型名
     */
    private String model;

    /**
     * 时间戳
     */
    private String timestamp;
}
