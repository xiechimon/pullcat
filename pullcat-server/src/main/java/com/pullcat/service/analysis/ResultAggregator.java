package com.pullcat.service.analysis;

import com.pullcat.dto.resp.AnalysisResultRespDTO;
import com.pullcat.dto.resp.IssueRespDTO;

import java.util.List;

public interface ResultAggregator {

    List<IssueRespDTO> mergeIssues(List<List<IssueRespDTO>> chunkResults);

    List<IssueRespDTO> mergeResults(List<AnalysisResultRespDTO> results);
}
