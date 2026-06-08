package com.pullcat.dto.resp;

import lombok.Data;

import java.util.List;

/**
 * PR 完整数据聚合，包含元数据、diff 文本、文件内容列表和目录树。
 */
@Data
public class PRDataRespDTO {

    /**
     * PR 元数据
     */
    private PRMetadataRespDTO metadata;

    /**
     * unified diff 文本
     */
    private String diff;

    /**
     * 变更文件完整内容列表
     */
    private List<FileContentRespDTO> files;

    /**
     * 格式化目录树字符串
     */
    private String fileTree;
}
