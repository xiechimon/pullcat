package com.pullcat.service.analysis.impl;

import com.pullcat.dto.resp.FileContentRespDTO;
import com.pullcat.service.analysis.TokenBudgetManager;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Token 预算管理器，估算文本 Token 消耗并在超出预算时按文件粒度分块
 */
@Component
public class TokenBudgetManagerImpl implements TokenBudgetManager {

    private static final int DEFAULT_MAX_TOKENS = 100_000;
    private static final int CHARS_PER_TOKEN = 4;

    private final int maxTokens;

    public TokenBudgetManagerImpl() {
        this.maxTokens = DEFAULT_MAX_TOKENS;
    }

    public TokenBudgetManagerImpl(int maxTokens) {
        this.maxTokens = maxTokens;
    }

    @Override
    public int getMaxTokens() {
        return maxTokens;
    }

    @Override
    public boolean isWithinBudget(String context) {
        return estimateTokens(context) <= maxTokens;
    }

    @Override
    public int estimateTokens(String text) {
        if (text == null || text.isEmpty()) {
            return 0;
        }
        return text.length() / CHARS_PER_TOKEN;
    }

    @Override
    public List<List<FileContentRespDTO>> chunkFiles(String headerInfo, List<FileContentRespDTO> files) {
        List<List<FileContentRespDTO>> chunks = new ArrayList<>();
        int headerTokens = estimateTokens(headerInfo);
        int remainingBudget = maxTokens - headerTokens;

        List<FileContentRespDTO> currentChunk = new ArrayList<>();
        int currentTokens = 0;

        for (FileContentRespDTO file : files) {
            if (file.isExcluded()) {
                continue;
            }
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
