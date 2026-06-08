package com.pullcat.dto.req;

import lombok.Data;

/**
 * 自动发布开关请求 DTO
 */
@Data
public class AutoPublishToggleReqDTO {

    /**
     * 是否启用自动发布
     */
    private Boolean enabled;
}
