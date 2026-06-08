package com.pullcat.dto.resp;

import lombok.Data;

/**
 * SSE PR 信息事件响应 DTO
 */
@Data
public class SsePrInfoRespDTO {

    /**
     * PR 地址
     */
    private String prUrl;

    /**
     * PR 元数据
     */
    private PRMetadataRespDTO metadata;

    /**
     * Diff 内容
     */
    private String diff;
}
