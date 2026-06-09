package com.pullcat.service.analysis;

import com.pullcat.common.enums.AnalysisType;
import com.pullcat.common.enums.Severity;
import com.pullcat.dto.resp.AnalysisResultRespDTO;
import com.pullcat.dto.resp.IssueRespDTO;
import com.pullcat.service.analysis.impl.ResultAggregatorImpl;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ResultAggregatorTest {

    private final ResultAggregator aggregator = new ResultAggregatorImpl();

    @Test
    void mergeResults_mergesDuplicateIssuesAndPreservesHighestSeverity() {
        AnalysisResultRespDTO risk = new AnalysisResultRespDTO(AnalysisType.RISK);
        risk.setIssues(List.of(issue("Null pointer risk", Severity.HIGH, "short")));

        AnalysisResultRespDTO quality = new AnalysisResultRespDTO(AnalysisType.QUALITY);
        quality.setIssues(List.of(issue("Null pointer risk", Severity.CRITICAL, "much longer description")));

        List<IssueRespDTO> merged = aggregator.mergeResults(List.of(risk, quality));

        assertEquals(1, merged.size());
        assertEquals(Severity.CRITICAL, merged.get(0).getSeverity());
        assertEquals(List.of("RISK", "QUALITY"), merged.get(0).getSourceDimensions());
        assertEquals("much longer description", merged.get(0).getDescription());
    }

    private IssueRespDTO issue(String title, Severity severity, String description) {
        IssueRespDTO issue = new IssueRespDTO();
        issue.setTitle(title);
        issue.setSeverity(severity);
        issue.setDescription(description);
        issue.setFile("src/Main.java");
        issue.setLine(12);
        return issue;
    }
}
