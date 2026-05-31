package com.pullcat.service.llm;

import com.pullcat.model.AnalysisResult;
import com.pullcat.model.AnalysisStatus;
import com.pullcat.model.AnalysisType;
import com.pullcat.model.Issue;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RiskAnalysisTest {

    @Test
    void getTemplateNameReturnsRisk() {
        RiskAnalysis analysis = new RiskAnalysis(null, null);
        assertThat(analysis.getTemplateName()).isEqualTo("risk");
    }

    @Test
    void getTypeReturnsRisk() {
        RiskAnalysis analysis = new RiskAnalysis(null, null);
        assertThat(analysis.getType()).isEqualTo(AnalysisType.RISK);
    }

    @Test
    void parseValidJsonResponse() {
        RiskAnalysis analysis = new RiskAnalysis(null, null);
        String response = """
                {
                  "summary": "Found risks",
                  "issues": [
                    {
                      "severity": "CRITICAL",
                      "file": "src/Login.java",
                      "line": 42,
                      "title": "SQL injection risk",
                      "description": "User input directly used in SQL query",
                      "suggestion": "Use PreparedStatement",
                      "confidence": 0.95
                    }
                  ]
                }""";

        List<Issue> issues = analysis.parseIssues(response);

        assertThat(issues).hasSize(1);
        Issue issue = issues.get(0);
        assertThat(issue.getSeverity()).isEqualTo(Issue.Severity.CRITICAL);
        assertThat(issue.getFile()).isEqualTo("src/Login.java");
        assertThat(issue.getLine()).isEqualTo(42);
        assertThat(issue.getTitle()).isEqualTo("SQL injection risk");
    }

    @Test
    void parseEmptyIssues() {
        RiskAnalysis analysis = new RiskAnalysis(null, null);
        String response = "{\"summary\": \"No risks\", \"issues\": []}";

        List<Issue> issues = analysis.parseIssues(response);

        assertThat(issues).isEmpty();
    }

    @Test
    void parseMalformedJsonReturnsEmpty() {
        RiskAnalysis analysis = new RiskAnalysis(null, null);

        List<Issue> issues = analysis.parseIssues("{broken json");

        assertThat(issues).isEmpty();
    }

    @Test
    void resultInitializedWithCorrectTypeAndModel() {
        QualityAnalysis analysis = new QualityAnalysis(null, "test-model");

        AnalysisResult result = analysis.getResult();

        assertThat(result.getType()).isEqualTo(AnalysisType.QUALITY);
        assertThat(result.getModel()).isEqualTo("test-model");
        assertThat(result.getStatus()).isEqualTo(AnalysisStatus.PENDING);
    }

    @Test
    void summaryAnalysisTemplate() {
        SummaryAnalysis analysis = new SummaryAnalysis(null, null);
        assertThat(analysis.getTemplateName()).isEqualTo("summary");
        assertThat(analysis.getType()).isEqualTo(AnalysisType.SUMMARY);
    }

    @Test
    void consistencyAnalysisTemplate() {
        ConsistencyAnalysis analysis = new ConsistencyAnalysis(null, null);
        assertThat(analysis.getTemplateName()).isEqualTo("consistency");
        assertThat(analysis.getType()).isEqualTo(AnalysisType.CONSISTENCY);
    }

    @Test
    void testingGapAnalysisTemplate() {
        TestingGapAnalysis analysis = new TestingGapAnalysis(null, null);
        assertThat(analysis.getTemplateName()).isEqualTo("testing");
        assertThat(analysis.getType()).isEqualTo(AnalysisType.TESTING);
    }

    @Test
    void qualityAnalysisTemplate() {
        QualityAnalysis analysis = new QualityAnalysis(null, null);
        assertThat(analysis.getTemplateName()).isEqualTo("quality");
        assertThat(analysis.getType()).isEqualTo(AnalysisType.QUALITY);
    }
}
