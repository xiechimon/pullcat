package com.pullcat.dto.req;

import lombok.Data;

/**
 * 问题反馈请求 DTO
 */
@Data
public class IssueFeedbackReqDTO {

    /**
     * 是否接受该问题
     */
    private Boolean accepted;

    /**
     * 反馈原因
     */
    private String reason;
}
