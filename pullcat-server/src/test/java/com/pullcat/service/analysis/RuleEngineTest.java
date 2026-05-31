package com.pullcat.service.analysis;

import com.pullcat.model.FileContent;
import com.pullcat.model.Issue;
import com.pullcat.model.Rule;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RuleEngineTest {

    private final RuleEngine ruleEngine = new RuleEngine();

    @Test
    void codePatternRuleMatchesContent() {
        Rule rule = createRule("missing-null-check", Rule.RuleType.CODE_PATTERN,
                "user\\.getName\\(\\)", Issue.Severity.HIGH,
                "Missing null check", "Add null check before calling getName()");

        FileContent file = new FileContent("src/UserService.java",
                "public String getDisplay() {\n    return user.getName();\n}", "");

        List<Issue> issues = ruleEngine.evaluate(List.of(file), List.of(rule));

        assertThat(issues).hasSize(1);
        Issue issue = issues.get(0);
        assertThat(issue.getSeverity()).isEqualTo(Issue.Severity.HIGH);
        assertThat(issue.getFile()).isEqualTo("src/UserService.java");
        assertThat(issue.getLine()).isEqualTo(2);
        assertThat(issue.getTitle()).isEqualTo("missing-null-check");
        assertThat(issue.getConfidence()).isEqualTo(1.0);
        assertThat(issue.getSourceDimensions()).contains("RULE_ENGINE");
    }

    @Test
    void codePatternRuleNoMatch() {
        Rule rule = createRule("sql-injection", Rule.RuleType.CODE_PATTERN,
                "Statement\\.executeQuery", Issue.Severity.CRITICAL,
                "SQL injection", "Use PreparedStatement");

        FileContent file = new FileContent("src/UserService.java",
                "ps.executeQuery();\nreturn result;", "");

        List<Issue> issues = ruleEngine.evaluate(List.of(file), List.of(rule));

        assertThat(issues).isEmpty();
    }

    @Test
    void filePathMatchRule() {
        Rule rule = createRule("no-test-files", Rule.RuleType.FILE_PATH_MATCH,
                "src/test/", Issue.Severity.LOW,
                "Test directory change", "Verify test changes");

        FileContent file = new FileContent("src/test/UserServiceTest.java",
                "class UserServiceTest {}", "");

        List<Issue> issues = ruleEngine.evaluate(List.of(file), List.of(rule));

        assertThat(issues).hasSize(1);
        assertThat(issues.get(0).getFile()).isEqualTo("src/test/UserServiceTest.java");
    }

    @Test
    void forbiddenApiRule() {
        Rule rule = createRule("no-system-exit", Rule.RuleType.FORBIDDEN_API,
                "System\\.exit", Issue.Severity.CRITICAL,
                "System.exit call", "Use exception handling instead");

        FileContent file = new FileContent("src/App.java",
                "if (error) {\n    System.exit(1);\n}", "");

        List<Issue> issues = ruleEngine.evaluate(List.of(file), List.of(rule));

        assertThat(issues).hasSize(1);
        assertThat(issues.get(0).getSeverity()).isEqualTo(Issue.Severity.CRITICAL);
    }

    @Test
    void disabledRuleIsSkipped() {
        Rule rule = createRule("check", Rule.RuleType.CODE_PATTERN,
                ".*", Issue.Severity.HIGH, "Check", "Fix");
        rule.setEnabled(false);

        FileContent file = new FileContent("src/App.java", "anything", "");

        List<Issue> issues = ruleEngine.evaluate(List.of(file), List.of(rule));

        assertThat(issues).isEmpty();
    }

    @Test
    void excludedFileIsSkipped() {
        Rule rule = createRule("check", Rule.RuleType.CODE_PATTERN,
                ".*", Issue.Severity.HIGH, "Check", "Fix");

        FileContent file = new FileContent("src/App.java", "anything", "");
        file.setExcluded(true);

        List<Issue> issues = ruleEngine.evaluate(List.of(file), List.of(rule));

        assertThat(issues).isEmpty();
    }

    @Test
    void multipleMatchesInSameFile() {
        Rule rule = createRule("println", Rule.RuleType.CODE_PATTERN,
                "System\\.out\\.println", Issue.Severity.LOW,
                "Avoid println", "Use logger instead");

        FileContent file = new FileContent("src/Debug.java",
                "System.out.println(\"a\");\nint x = 1;\nSystem.out.println(\"b\");", "");

        List<Issue> issues = ruleEngine.evaluate(List.of(file), List.of(rule));

        assertThat(issues).hasSize(2);
        assertThat(issues.get(0).getLine()).isEqualTo(1);
        assertThat(issues.get(1).getLine()).isEqualTo(3);
    }

    @Test
    void nullFileContentIsSkipped() {
        Rule rule = createRule("check", Rule.RuleType.CODE_PATTERN,
                ".*", Issue.Severity.HIGH, "Check", "Fix");

        FileContent file = new FileContent("src/App.java", null, "");

        List<Issue> issues = ruleEngine.evaluate(List.of(file), List.of(rule));

        assertThat(issues).isEmpty();
    }

    @Test
    void multipleRulesAgainstSameFile() {
        Rule rule1 = createRule("rule1", Rule.RuleType.CODE_PATTERN,
                "TODO", Issue.Severity.LOW, "TODO found", "Remove TODO");
        Rule rule2 = createRule("rule2", Rule.RuleType.CODE_PATTERN,
                "FIXME", Issue.Severity.MEDIUM, "FIXME found", "Fix the issue");

        FileContent file = new FileContent("src/App.java",
                "// TODO implement\n// FIXME bug here", "");

        List<Issue> issues = ruleEngine.evaluate(List.of(file), List.of(rule1, rule2));

        assertThat(issues).hasSize(2);
    }

    private static Rule createRule(String name, Rule.RuleType type, String pattern,
                                   Issue.Severity severity, String message, String suggestion) {
        Rule rule = new Rule();
        rule.setName(name);
        rule.setType(type);
        rule.setPattern(pattern);
        rule.setSeverity(severity);
        rule.setMessage(message);
        rule.setSuggestion(suggestion);
        rule.setEnabled(true);
        return rule;
    }
}
