package com.pullcat.dto.resp;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * SSE 分析启动事件响应 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SseAnalysisStartedRespDTO {

    /**
     * 启动任务列表
     */
    private List<String> tasks;
}
