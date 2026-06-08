package com.pullcat.dto.resp;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 常见问题类型统计响应 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CommonIssueTypeRespDTO {

    /**
     * 问题类型
     */
    private String type;

    /**
     * 命中次数
     */
    private Integer count;
}
