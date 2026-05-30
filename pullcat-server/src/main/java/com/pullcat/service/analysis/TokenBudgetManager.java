package com.pullcat.service.analysis;

import com.pullcat.model.FileContent;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Token 预算管理器，估算文本 Token 消耗并在超出预算时按文件粒度分块。
 */
@Component
public class TokenBudgetManager {

    private static final int DEFAULT_MAX_TOKENS = 100_000;

    private static final int CHARS_PER_TOKEN = 4;

    private final int maxTokens;

    public TokenBudgetManager() {
        this.maxTokens = DEFAULT_MAX_TOKENS;
    }

    public TokenBudgetManager(int maxTokens) {
        this.maxTokens = maxTokens;
    }

    public int getMaxTokens() {
        return maxTokens;
    }

    /**
     * 判断上下文是否在 Token 预算内。
     */
    public boolean isWithinBudget(String context) {
        return estimateTokens(context) <= maxTokens;
    }

    /**
     * 按字符数 / 4 估算 Token 数。
     */
    public int estimateTokens(String text) {
        if (text == null || text.isEmpty()) return 0;
        return text.length() / CHARS_PER_TOKEN;
    }

    /**
     * 当内容超出 Token 预算时按文件粒度分块，每块包含头部信息。
     */
    public List<List<FileContent>> chunkFiles(String headerInfo, List<FileContent> files) {
        List<List<FileContent>> chunks = new ArrayList<>();
        int headerTokens = estimateTokens(headerInfo);
        int remainingBudget = maxTokens - headerTokens;

        List<FileContent> currentChunk = new ArrayList<>();
        int currentTokens = 0;

        for (FileContent file : files) {
            if (file.isExcluded()) continue;
            int fileTokens = estimateTokens(file.getContent()) + estimateTokens(file.getDiff());

            if (currentTokens + fileTokens > remainingBudget && !currentChunk.isEmpty()) {
                chunks.add(currentChunk);
                currentChunk = new ArrayList<>();
                currentTokens = 0;
            }

            currentChunk.add(file);
            currentTokens += fileTokens;
        }

        if (!currentChunk.isEmpty()) {
            chunks.add(currentChunk);
        }

        return chunks;
    }
}
