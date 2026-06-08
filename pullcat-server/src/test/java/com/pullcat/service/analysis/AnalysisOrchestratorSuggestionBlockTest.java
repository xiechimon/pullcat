package com.pullcat.service.analysis;

import com.pullcat.common.enums.Severity;
import com.pullcat.dto.resp.IssueRespDTO;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AnalysisOrchestratorSuggestionBlockTest {

    @Test
    void buildSuggestionBlockFormatsCorrectly() throws Exception {
        AnalysisOrchestrator orchestrator = new AnalysisOrchestrator(
                null, null, null, null, null, null, null, null, null, null, null, null);

        IssueRespDTO issue = new IssueRespDTO();
        issue.setSeverity(Severity.HIGH);
        issue.setTitle("NPE risk");
        issue.setFile("src/main/Foo.java");
        issue.setLine(42);
        issue.setDescription("Potential null pointer when user is not authenticated");
        issue.setSuggestionCode("Optional.ofNullable(user).map(UserProfile::getName).orElse(\"unknown\")");

        Method method = AnalysisOrchestrator.class.getDeclaredMethod("buildSuggestionBlock", IssueRespDTO.class);
        method.setAccessible(true);
        String result = (String) method.invoke(orchestrator, issue);

        assertThat(result).contains("**" + "[HIGH] NPE risk**");
        assertThat(result).contains("Potential null pointer when user is not authenticated");
        assertThat(result).contains("```suggestion");
        assertThat(result).contains("Optional.ofNullable(user).map(UserProfile::getName).orElse(\"unknown\")");
    }

    @Test
    void buildSuggestionBlockHandlesNullLine() throws Exception {
        AnalysisOrchestrator orchestrator = new AnalysisOrchestrator(
                null, null, null, null, null, null, null, null, null, null, null, null);

        IssueRespDTO issue = new IssueRespDTO();
        issue.setSeverity(Severity.MEDIUM);
        issue.setTitle("Missing docs");
        issue.setFile("src/Util.java");
        issue.setLine(null);
        issue.setDescription("No javadoc on public method");
        issue.setSuggestionCode("/** Returns the value. */\npublic String getValue() { return value; }");

        Method method = AnalysisOrchestrator.class.getDeclaredMethod("buildSuggestionBlock", IssueRespDTO.class);
        method.setAccessible(true);
        String result = (String) method.invoke(orchestrator, issue);

        assertThat(result).contains("**" + "[MEDIUM] Missing docs**");
        assertThat(result).contains("```suggestion");
    }

    @Test
    void buildPublishSummaryMentionsInlineFixesInsteadOfEmbeddedBlocks() throws Exception {
        ResultAggregator aggregator = new ResultAggregator();
        AnalysisOrchestrator orchestrator = new AnalysisOrchestrator(
                null, null, null, null, null, null, null, aggregator, null, null, null, null);

        var session = new com.pullcat.dto.resp.ReviewSessionRespDTO();
        session.setId("test-session");
        session.setPrUrl("https://github.com/owner/repo/pull/1");

        var summaryResult = new com.pullcat.dto.resp.AnalysisResultRespDTO();
        summaryResult.setType(com.pullcat.common.enums.AnalysisType.SUMMARY);
        summaryResult.setContent("{\"summary\": \"This PR adds login feature\"}");
        session.getAnalyses().put("summary", summaryResult);

        IssueRespDTO issue = new IssueRespDTO();
        issue.setSeverity(Severity.CRITICAL);
        issue.setTitle("SQL injection");
        issue.setFile("src/LoginService.java");
        issue.setLine(25);
        issue.setDescription("Unsanitized input in SQL query");
        issue.setSuggestionCode("PreparedStatement ps = conn.prepareStatement(\"SELECT * FROM users WHERE name = ?\");");

        var riskResult = new com.pullcat.dto.resp.AnalysisResultRespDTO();
        riskResult.setType(com.pullcat.common.enums.AnalysisType.RISK);
        riskResult.setIssues(List.of(issue));
        session.getAnalyses().put("risk", riskResult);

        Method method = AnalysisOrchestrator.class.getDeclaredMethod("buildPublishSummary",
                List.class, com.pullcat.dto.resp.ReviewSessionRespDTO.class);
        method.setAccessible(true);

        List<IssueRespDTO> merged = aggregator.mergeResults(
                List.copyOf(session.getAnalyses().values()));
        String summary = (String) method.invoke(orchestrator, merged, session);

        assertThat(summary).contains("### 修复建议");
        assertThat(summary).contains("inline");
        assertThat(summary).contains("一键应用");
    }
}
