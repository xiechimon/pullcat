package com.pullcat.service.analysis;

import com.pullcat.common.enums.AnalysisStatus;
import com.pullcat.common.enums.AnalysisType;
import com.pullcat.dto.resp.AnalysisResultRespDTO;
import com.pullcat.service.analysis.impl.AnalysisOrchestratorImpl;
import com.pullcat.service.llm.AnalysisService;
import com.pullcat.service.llm.AnalysisTask;
import com.pullcat.service.llm.impl.ConsistencyAnalysisServiceImpl;
import com.pullcat.service.llm.impl.QualityAnalysisServiceImpl;
import com.pullcat.service.llm.impl.RiskAnalysisServiceImpl;
import com.pullcat.service.llm.impl.SummaryAnalysisServiceImpl;
import com.pullcat.service.llm.impl.TestingGapAnalysisServiceImpl;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.test.util.ReflectionTestUtils;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AnalysisOrchestratorImplTest {

    @Test
    void createTask_usesRenamedLlmImplementations() {
        ExecutorService executorService = Executors.newSingleThreadExecutor();
        try {
            AnalysisOrchestratorImpl orchestrator = new AnalysisOrchestratorImpl(
                    Mockito.mock(com.pullcat.remote.GitHubApiService.class),
                    Mockito.mock(PromptLoader.class),
                    Mockito.mock(ContextBuilder.class),
                    Mockito.mock(ReviewRepository.class),
                    Mockito.mock(ChatClient.class),
                    Mockito.mock(ChatClient.class),
                    executorService,
                    Mockito.mock(ResultAggregator.class),
                    Mockito.mock(RuleEngine.class),
                    Mockito.mock(RuleRepository.class),
                    Mockito.mock(RuleSuggestionService.class),
                    new SimpleMeterRegistry()
            );

            ReflectionTestUtils.setField(orchestrator, "lightModelName", "light-model");
            ReflectionTestUtils.setField(orchestrator, "heavyModelName", "heavy-model");

            assertTaskType(orchestrator, AnalysisType.SUMMARY, SummaryAnalysisServiceImpl.class, "summary");
            assertTaskType(orchestrator, AnalysisType.RISK, RiskAnalysisServiceImpl.class, "risk");
            assertTaskType(orchestrator, AnalysisType.QUALITY, QualityAnalysisServiceImpl.class, "quality");
            assertTaskType(orchestrator, AnalysisType.CONSISTENCY, ConsistencyAnalysisServiceImpl.class, "consistency");
            assertTaskType(orchestrator, AnalysisType.TESTING, TestingGapAnalysisServiceImpl.class, "testing");
        } finally {
            executorService.shutdownNow();
        }
    }

    @Test
    void executeTask_usesTaskTemplateNameInsteadOfAnalysisTypeTemplateName() {
        ExecutorService executorService = Executors.newSingleThreadExecutor();
        try {
            PromptLoader promptLoader = Mockito.mock(PromptLoader.class);
            when(promptLoader.loadTemplate("custom-template")).thenReturn("template body");
            when(promptLoader.populateTemplate("template body", Map.of("key", "value"))).thenReturn("final prompt");

            AnalysisOrchestratorImpl orchestrator = new AnalysisOrchestratorImpl(
                    Mockito.mock(com.pullcat.remote.GitHubApiService.class),
                    promptLoader,
                    Mockito.mock(ContextBuilder.class),
                    Mockito.mock(ReviewRepository.class),
                    Mockito.mock(ChatClient.class),
                    Mockito.mock(ChatClient.class),
                    executorService,
                    Mockito.mock(ResultAggregator.class),
                    Mockito.mock(RuleEngine.class),
                    Mockito.mock(RuleRepository.class),
                    Mockito.mock(RuleSuggestionService.class),
                    new SimpleMeterRegistry()
            );

            AnalysisTask task = new StubAnalysisTask();

            AnalysisResultRespDTO result = (AnalysisResultRespDTO) ReflectionTestUtils.invokeMethod(
                    orchestrator,
                    "executeTask",
                    task,
                    Map.of("key", "value"),
                    "missing-session"
            );

            verify(promptLoader).loadTemplate("custom-template");
            verify(promptLoader).populateTemplate("template body", Map.of("key", "value"));
            assertEquals(AnalysisType.SUMMARY, result.getType());
            assertEquals(AnalysisStatus.COMPLETED, result.getStatus());
            assertEquals("final prompt", result.getContent());
        } finally {
            executorService.shutdownNow();
        }
    }

    private void assertTaskType(AnalysisOrchestratorImpl orchestrator,
                                AnalysisType analysisType,
                                Class<? extends AnalysisTask> expectedType,
                                String expectedTemplateName) {
        Object task = ReflectionTestUtils.invokeMethod(orchestrator, "createTask", analysisType);
        assertInstanceOf(expectedType, task);
        assertInstanceOf(AnalysisService.class, task);
        assertEquals(analysisType, ((AnalysisService) task).getType());
        assertEquals(expectedTemplateName, ((AnalysisService) task).getTemplateName());
    }

    private static final class StubAnalysisTask extends AnalysisTask implements AnalysisService {

        private StubAnalysisTask() {
            super(Mockito.mock(ChatClient.class, Mockito.RETURNS_DEEP_STUBS), "stub-model", AnalysisType.SUMMARY);
        }

        @Override
        public Mono<AnalysisResultRespDTO> execute(String prompt) {
            AnalysisResultRespDTO result = getResult();
            result.setStatus(AnalysisStatus.COMPLETED);
            result.setContent(prompt);
            return Mono.just(result);
        }

        @Override
        public String getTemplateName() {
            return "custom-template";
        }
    }
}
