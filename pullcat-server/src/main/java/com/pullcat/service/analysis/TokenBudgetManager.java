package com.pullcat.service.analysis;

import com.pullcat.dto.resp.FileContentRespDTO;

import java.util.List;

public interface TokenBudgetManager {

    int getMaxTokens();

    boolean isWithinBudget(String context);

    int estimateTokens(String text);

    List<List<FileContentRespDTO>> chunkFiles(String headerInfo, List<FileContentRespDTO> files);
}
