package com.pullcat.service.analysis;

import com.pullcat.model.Issue;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;

class AnalysisOrchestratorSuggestionBlockTest {

    @Test
    void buildSuggestionBlockFormatsCorrectly() throws Exception {
        AnalysisOrchestrator orchestrator = new AnalysisOrchestrator(
                null, null, null, null, null, null, null, null, null, null, null);

        Issue issue = new Issue();
        issue.setSeverity(Issue.Severity.HIGH);
        issue.setTitle("NPE risk");
        issue.setFile("src/main/Foo.java");
        issue.setLine(42);
        issue.setDescription("Potential null pointer when user is not authenticated");
        issue.setSuggestionCode("Optional.ofNullable(user).map(User::getName).orElse(\"unknown\")");

        Method method = AnalysisOrchestrator.class.getDeclaredMethod("buildSuggestionBlock", Issue.class);
        method.setAccessible(true);
        String result = (String) method.invoke(orchestrator, issue);

        assertThat(result).contains("#### [HIGH] NPE risk - `src/main/Foo.java:42`");
        assertThat(result).contains("Potential null pointer when user is not authenticated");
        assertThat(result).contains("```suggestion");
        assertThat(result).contains("Optional.ofNullable(user).map(User::getName).orElse(\"unknown\")");
    }

    @Test
    void buildSuggestionBlockHandlesNullLine() throws Exception {
        AnalysisOrchestrator orchestrator = new AnalysisOrchestrator(
                null, null, null, null, null, null, null, null, null, null, null);

        Issue issue = new Issue();
        issue.setSeverity(Issue.Severity.MEDIUM);
        issue.setTitle("Missing docs");
        issue.setFile("src/Util.java");
        issue.setLine(null);
        issue.setDescription("No javadoc on public method");
        issue.setSuggestionCode("/** Returns the value. */\npublic String getValue() { return value; }");

        Method method = AnalysisOrchestrator.class.getDeclaredMethod("buildSuggestionBlock", Issue.class);
        method.setAccessible(true);
        String result = (String) method.invoke(orchestrator, issue);

        assertThat(result).contains("`src/Util.java:null`");
        assertThat(result).contains("```suggestion");
    }

    @Test
    void buildPublishSummaryIncludesSuggestionBlocks() throws Exception {
        ResultAggregator aggregator = new ResultAggregator();
        AnalysisOrchestrator orchestrator = new AnalysisOrchestrator(
                null, null, null, null, null, null, null, aggregator, null, null, null);

        var session = new com.pullcat.model.ReviewSession();
        session.setId("test-session");
        session.setPrUrl("https://github.com/owner/repo/pull/1");

        var summaryResult = new com.pullcat.model.AnalysisResult();
        summaryResult.setType(com.pullcat.model.AnalysisType.SUMMARY);
        summaryResult.setContent("{\"summary\": \"This PR adds login feature\"}");
        session.getAnalyses().put("summary", summaryResult);

        Issue issue = new Issue();
        issue.setSeverity(Issue.Severity.CRITICAL);
        issue.setTitle("SQL injection");
        issue.setFile("src/LoginService.java");
        issue.setLine(25);
        issue.setDescription("Unsanitized input in SQL query");
        issue.setSuggestionCode("PreparedStatement ps = conn.prepareStatement(\"SELECT * FROM users WHERE name = ?\");");

        var riskResult = new com.pullcat.model.AnalysisResult();
        riskResult.setType(com.pullcat.model.AnalysisType.RISK);
        riskResult.setIssues(java.util.List.of(issue));
        session.getAnalyses().put("risk", riskResult);

        Method method = AnalysisOrchestrator.class.getDeclaredMethod("buildPublishSummary",
                com.pullcat.model.ReviewSession.class);
        method.setAccessible(true);
        String summary = (String) method.invoke(orchestrator, session);

        assertThat(summary).contains("### Suggested Fixes");
        assertThat(summary).contains("```suggestion");
        assertThat(summary).contains("PreparedStatement ps = conn.prepareStatement");
    }
}
