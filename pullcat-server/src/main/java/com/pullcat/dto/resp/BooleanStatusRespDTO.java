package com.pullcat.dto.resp;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 通用布尔状态响应 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BooleanStatusRespDTO {

    /**
     * 状态值
     */
    private boolean enabled;
}
