package com.pullcat.service.analysis;

import com.pullcat.model.Issue;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ResultAggregatorTest {

    private final ResultAggregator aggregator = new ResultAggregator();

    private Issue issue(String id, Issue.Severity severity, String file, Integer line, String title) {
        return new Issue(id, severity, file, line, title, "desc", "suggestion", 0.8);
    }

    @Test
    void mergeSimpleList() {
        List<Issue> chunk1 = List.of(
                issue("1", Issue.Severity.HIGH, "a.java", 1, "High issue"),
                issue("2", Issue.Severity.LOW, "b.java", 2, "Low issue")
        );

        List<List<Issue>> chunks = List.of(chunk1);
        List<Issue> result = aggregator.mergeIssues(chunks);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getSeverity()).isEqualTo(Issue.Severity.HIGH);
        assertThat(result.get(1).getSeverity()).isEqualTo(Issue.Severity.LOW);
    }

    @Test
    void mergeDeduplicates() {
        Issue i1 = issue("1", Issue.Severity.MEDIUM, "a.java", 10, "Same title");
        Issue i2 = issue("2", Issue.Severity.MEDIUM, "a.java", 10, "Same title");

        List<List<Issue>> chunks = List.of(List.of(i1), List.of(i2));
        List<Issue> result = aggregator.mergeIssues(chunks);

        assertThat(result).hasSize(1);
    }

    @Test
    void mergeSortsBySeverity() {
        Issue critical = issue("c", Issue.Severity.CRITICAL, "a", 1, "C");
        Issue low = issue("l", Issue.Severity.LOW, "b", 2, "L");
        Issue medium = issue("m", Issue.Severity.MEDIUM, "c", 3, "M");

        List<List<Issue>> chunks = List.of(List.of(low, critical, medium));
        List<Issue> result = aggregator.mergeIssues(chunks);

        assertThat(result.get(0).getSeverity()).isEqualTo(Issue.Severity.CRITICAL);
        assertThat(result.get(1).getSeverity()).isEqualTo(Issue.Severity.MEDIUM);
        assertThat(result.get(2).getSeverity()).isEqualTo(Issue.Severity.LOW);
    }

    @Test
    void mergeHandlesNullChunks() {
        List<Issue> chunk = List.of(issue("1", Issue.Severity.INFO, "a", 1, "test"));
        List<List<Issue>> chunks = new java.util.ArrayList<>();
        chunks.add(chunk);
        chunks.add(null);

        List<Issue> result = aggregator.mergeIssues(chunks);

        assertThat(result).hasSize(1);
    }

    @Test
    void mergeEmptyInput() {
        List<Issue> result = aggregator.mergeIssues(List.of());

        assertThat(result).isEmpty();
    }
}
