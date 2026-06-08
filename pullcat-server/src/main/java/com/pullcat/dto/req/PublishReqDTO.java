package com.pullcat.dto.req;

import lombok.Data;

import java.util.List;

/**
 * 发布请求 DTO，用户选择要发布的问题列表。
 */
@Data
public class PublishReqDTO {
    /** 是否包含审查摘要 */
    private boolean includeSummary;
    /** 选中的问题 ID 列表 */
    private List<String> selectedIssueIds;
}
