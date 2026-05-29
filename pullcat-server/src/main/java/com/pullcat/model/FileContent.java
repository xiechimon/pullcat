package com.pullcat.model;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 文件内容模型，封装单个文件的路径、内容、diff 及排除状态。
 */
@Data
@NoArgsConstructor
public class FileContent {
    /**
     * 文件路径
     */
    private String path;

    /**
     * 文件完整内容
     */
    private String content;

    /**
     * 文件的diff内容
     */
    private String diff;

    /**
     * 是否在分析中排除该文件
     */
    private boolean excluded;

    /**
     * 创建文件内容实例，excluded 默认为 false。
     *
     * @param path    文件路径
     * @param content 文件完整内容
     * @param diff    文件的 diff 内容
     */
    public FileContent(String path, String content, String diff) {
        this.path = path;
        this.content = content;
        this.diff = diff;
        this.excluded = false;
    }
}
