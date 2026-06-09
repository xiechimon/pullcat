package com.pullcat.service.llm;

import com.pullcat.common.enums.AnalysisStatus;
import com.pullcat.common.enums.AnalysisType;
import com.pullcat.common.enums.Severity;
import com.pullcat.dto.resp.AnalysisResultRespDTO;
import com.pullcat.service.llm.impl.SummaryAnalysisServiceImpl;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;
import org.springframework.ai.chat.client.ChatClient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SummaryAnalysisServiceImplTest {

    @Test
    void execute_parsesIssuesAndUsesSummaryMetadata() {
        ChatClient chatClient = mock(ChatClient.class, Answers.RETURNS_DEEP_STUBS);
        String prompt = "summarize this pr";
        String response = """
                {
                  "summary": "summary text",
                  "issues": [
                    {
                      "severity": "high",
                      "file": "src/Main.java",
                      "line": 12,
                      "title": "Null risk",
                      "description": "Possible null access",
                      "suggestion": "Add guard",
                      "confidence": 0.91
                    }
                  ]
                }
                """;
        when(chatClient.prompt().user(prompt).call().content()).thenReturn(response);

        AnalysisService service = new SummaryAnalysisServiceImpl(chatClient, "mock-model");

        AnalysisResultRespDTO result = service.execute(prompt).block();

        assertNotNull(result);
        assertEquals(AnalysisType.SUMMARY, service.getType());
        assertEquals("summary", service.getTemplateName());
        assertEquals(AnalysisStatus.COMPLETED, result.getStatus());
        assertEquals("mock-model", result.getModel());
        assertEquals(response, result.getContent());
        assertEquals(1, result.getIssues().size());
        assertEquals("SUMMARY-1", result.getIssues().get(0).getId());
        assertEquals(Severity.HIGH, result.getIssues().get(0).getSeverity());
        assertEquals("Null risk", result.getIssues().get(0).getTitle());
        assertTrue(result.getStartedAt() != null);
        assertTrue(result.getCompletedAt() != null);
    }
}
