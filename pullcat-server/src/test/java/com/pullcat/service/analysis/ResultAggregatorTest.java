package com.pullcat.service.analysis;

import com.pullcat.dto.resp.AnalysisResultRespDTO;
import com.pullcat.common.enums.AnalysisType;
import com.pullcat.common.enums.Severity;
import com.pullcat.dto.resp.IssueRespDTO;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ResultAggregatorTest {

    private final ResultAggregator aggregator = new ResultAggregator();

    private IssueRespDTO issue(String id, Severity severity, String file, Integer line, String title) {
        return new IssueRespDTO(id, severity, file, line, title, "desc", "suggestion", 0.8);
    }

    private AnalysisResultRespDTO result(AnalysisType type, List<IssueRespDTO> issues) {
        AnalysisResultRespDTO r = new AnalysisResultRespDTO(type);
        r.setIssues(issues);
        return r;
    }

    @Test
    void mergeSimpleList() {
        List<IssueRespDTO> chunk1 = List.of(
                issue("1", Severity.HIGH, "a.java", 1, "High issue"),
                issue("2", Severity.LOW, "b.java", 2, "Low issue")
        );

        List<List<IssueRespDTO>> chunks = List.of(chunk1);
        List<IssueRespDTO> result = aggregator.mergeIssues(chunks);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getSeverity()).isEqualTo(Severity.HIGH);
        assertThat(result.get(1).getSeverity()).isEqualTo(Severity.LOW);
    }

    @Test
    void mergeDeduplicates() {
        IssueRespDTO i1 = issue("1", Severity.MEDIUM, "a.java", 10, "Same title");
        IssueRespDTO i2 = issue("2", Severity.MEDIUM, "a.java", 10, "Same title");

        List<List<IssueRespDTO>> chunks = List.of(List.of(i1), List.of(i2));
        List<IssueRespDTO> result = aggregator.mergeIssues(chunks);

        assertThat(result).hasSize(1);
    }

    @Test
    void mergeSortsBySeverity() {
        IssueRespDTO critical = issue("c", Severity.CRITICAL, "a", 1, "C");
        IssueRespDTO low = issue("l", Severity.LOW, "b", 2, "L");
        IssueRespDTO medium = issue("m", Severity.MEDIUM, "c", 3, "M");

        List<List<IssueRespDTO>> chunks = List.of(List.of(low, critical, medium));
        List<IssueRespDTO> result = aggregator.mergeIssues(chunks);

        assertThat(result.get(0).getSeverity()).isEqualTo(Severity.CRITICAL);
        assertThat(result.get(1).getSeverity()).isEqualTo(Severity.MEDIUM);
        assertThat(result.get(2).getSeverity()).isEqualTo(Severity.LOW);
    }

    @Test
    void mergeHandlesNullChunks() {
        List<IssueRespDTO> chunk = List.of(issue("1", Severity.INFO, "a", 1, "test"));
        List<List<IssueRespDTO>> chunks = new java.util.ArrayList<>();
        chunks.add(chunk);
        chunks.add(null);

        List<IssueRespDTO> result = aggregator.mergeIssues(chunks);

        assertThat(result).hasSize(1);
    }

    @Test
    void mergeEmptyInput() {
        List<IssueRespDTO> result = aggregator.mergeIssues(List.of());

        assertThat(result).isEmpty();
    }

    @Test
    void mergeResultsDeduplicatesAcrossDimensions() {
        IssueRespDTO riskIssue = issue("R1", Severity.CRITICAL, "a.java", 10, "Null check missing");
        IssueRespDTO qualityIssue = issue("Q1", Severity.HIGH, "a.java", 10, "Null check missing");

        List<AnalysisResultRespDTO> results = List.of(
                result(AnalysisType.RISK, List.of(riskIssue)),
                result(AnalysisType.QUALITY, List.of(qualityIssue))
        );

        List<IssueRespDTO> merged = aggregator.mergeResults(results);

        assertThat(merged).hasSize(1);
        assertThat(merged.get(0).getSeverity()).isEqualTo(Severity.CRITICAL);
        assertThat(merged.get(0).getSourceDimensions()).contains("RISK", "QUALITY");
    }

    @Test
    void mergeResultsKeepsSeparateDifferentFiles() {
        IssueRespDTO a = issue("1", Severity.HIGH, "a.java", 10, "IssueRespDTO A");
        IssueRespDTO b = issue("2", Severity.HIGH, "b.java", 10, "IssueRespDTO B");

        List<AnalysisResultRespDTO> results = List.of(
                result(AnalysisType.RISK, List.of(a)),
                result(AnalysisType.QUALITY, List.of(b))
        );

        List<IssueRespDTO> merged = aggregator.mergeResults(results);

        assertThat(merged).hasSize(2);
    }

    @Test
    void mergeResultsDifferentLinesStaySeparate() {
        IssueRespDTO a = issue("1", Severity.HIGH, "a.java", 10, "Same title");
        IssueRespDTO b = issue("2", Severity.HIGH, "a.java", 20, "Same title");

        List<AnalysisResultRespDTO> results = List.of(
                result(AnalysisType.RISK, List.of(a)),
                result(AnalysisType.QUALITY, List.of(b))
        );

        List<IssueRespDTO> merged = aggregator.mergeResults(results);

        assertThat(merged).hasSize(2);
    }

    @Test
    void mergeResultsHandlesEmptyInput() {
        List<IssueRespDTO> merged = aggregator.mergeResults(List.of());
        assertThat(merged).isEmpty();
    }

    @Test
    void mergeResultsHandlesNullResults() {
        List<IssueRespDTO> merged = aggregator.mergeResults(null);
        assertThat(merged).isEmpty();
    }

    @Test
    void mergeResultsSingleDimensionNoChange() {
        IssueRespDTO issue = issue("1", Severity.HIGH, "a.java", 10, "Single");
        List<AnalysisResultRespDTO> results = List.of(result(AnalysisType.RISK, List.of(issue)));

        List<IssueRespDTO> merged = aggregator.mergeResults(results);

        assertThat(merged).hasSize(1);
        assertThat(merged.get(0).getSourceDimensions()).containsExactly("RISK");
    }

    @Test
    void mergeResultsUsesMaxConfidence() {
        IssueRespDTO lowConf = issue("1", Severity.HIGH, "a.java", 10, "Same");
        lowConf.setConfidence(0.3);
        IssueRespDTO highConf = issue("2", Severity.HIGH, "a.java", 10, "Same");
        highConf.setConfidence(0.9);

        List<AnalysisResultRespDTO> results = List.of(
                result(AnalysisType.RISK, List.of(lowConf)),
                result(AnalysisType.QUALITY, List.of(highConf))
        );

        List<IssueRespDTO> merged = aggregator.mergeResults(results);

        assertThat(merged).hasSize(1);
        assertThat(merged.get(0).getConfidence()).isEqualTo(0.9);
    }
}
