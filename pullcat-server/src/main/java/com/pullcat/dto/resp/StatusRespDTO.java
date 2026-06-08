package com.pullcat.dto.resp;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 通用状态响应 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class StatusRespDTO {

    /**
     * 状态值
     */
    private String status;
}
