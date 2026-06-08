package com.pullcat.dto.resp;

import lombok.Data;

import java.util.List;

/**
 * 审查列表响应 DTO
 */
@Data
public class ReviewListRespDTO {

    /**
     * 当前页数据
     */
    private List<ReviewSessionRespDTO> items;

    /**
     * 总数
     */
    private long total;

    /**
     * 页码
     */
    private int page;

    /**
     * 每页大小
     */
    private int size;
}
