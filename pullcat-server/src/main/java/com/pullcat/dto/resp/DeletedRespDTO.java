package com.pullcat.dto.resp;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 通用删除结果响应 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DeletedRespDTO {

    /**
     * 是否已删除
     */
    private boolean deleted;
}
