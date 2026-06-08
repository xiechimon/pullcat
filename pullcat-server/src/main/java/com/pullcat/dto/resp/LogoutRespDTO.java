package com.pullcat.dto.resp;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 退出登录响应 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LogoutRespDTO {

    /**
     * 退出状态
     */
    private String status;
}
