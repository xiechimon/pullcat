package com.pullcat.service.llm;

import com.pullcat.model.Issue;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class JsonOutputParserTest {

    @Test
    void parseIssuesValidJson() {
        String response = """
                {
                  "summary": "test",
                  "issues": [
                    {
                      "severity": "CRITICAL",
                      "file": "src/Foo.java",
                      "line": 42,
                      "title": "NPE risk",
                      "description": "Potential null pointer",
                      "suggestion": "Add null check",
                      "confidence": 0.95
                    }
                  ]
                }""";

        List<Issue> issues = JsonOutputParser.parseIssues(response);

        assertThat(issues).hasSize(1);
        Issue issue = issues.get(0);
        assertThat(issue.getSeverity()).isEqualTo(Issue.Severity.CRITICAL);
        assertThat(issue.getFile()).isEqualTo("src/Foo.java");
        assertThat(issue.getLine()).isEqualTo(42);
        assertThat(issue.getTitle()).isEqualTo("NPE risk");
        assertThat(issue.getDescription()).isEqualTo("Potential null pointer");
        assertThat(issue.getSuggestion()).isEqualTo("Add null check");
        assertThat(issue.getConfidence()).isEqualTo(0.95);
    }

    @Test
    void parseIssuesEmptyArray() {
        String response = "{\"summary\": \"ok\", \"issues\": []}";

        List<Issue> issues = JsonOutputParser.parseIssues(response);

        assertThat(issues).isEmpty();
    }

    @Test
    void parseIssuesNoIssuesKey() {
        String response = "{\"summary\": \"just a summary\"}";

        List<Issue> issues = JsonOutputParser.parseIssues(response);

        assertThat(issues).isEmpty();
    }

    @Test
    void parseIssuesMalformedJson() {
        List<Issue> issues = JsonOutputParser.parseIssues("not json at all");

        assertThat(issues).isEmpty();
    }

    @Test
    void parseIssuesWithCodeFence() {
        String response = """
                ```json
                {"issues": [{"severity": "LOW", "title": "minor"}]}
                ```""";

        List<Issue> issues = JsonOutputParser.parseIssues(response);

        assertThat(issues).hasSize(1);
        assertThat(issues.get(0).getSeverity()).isEqualTo(Issue.Severity.LOW);
    }

    @Test
    void parseIssuesUnknownSeverityDefaultsToInfo() {
        String response = """
                {"issues": [{"severity": "UNKNOWN", "title": "test"}]}""";

        List<Issue> issues = JsonOutputParser.parseIssues(response);

        assertThat(issues.get(0).getSeverity()).isEqualTo(Issue.Severity.INFO);
    }

    @Test
    void extractJsonStripsCodeFence() {
        String result = JsonOutputParser.extractJson("```json\n{\"a\":1}\n```");

        assertThat(result).isEqualTo("{\"a\":1}");
    }

    @Test
    void extractJsonFindsBraces() {
        String result = JsonOutputParser.extractJson("prefix {\"a\":1} suffix");

        assertThat(result).isEqualTo("{\"a\":1}");
    }
}
