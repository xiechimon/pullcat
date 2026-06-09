package com.pullcat.service.analysis;

import com.pullcat.common.enums.AnalysisStatus;
import com.pullcat.common.enums.AnalysisType;
import com.pullcat.dto.resp.AnalysisResultRespDTO;
import com.pullcat.service.analysis.impl.AnalysisOrchestratorImpl;
import com.pullcat.service.llm.AnalysisService;
import com.pullcat.service.llm.AnalysisTask;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.test.util.ReflectionTestUtils;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AnalysisOrchestratorImplTest {

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
                    Mockito.mock(ReviewSessionService.class),
                    Mockito.mock(AnalysisTaskFactory.class),
                    executorService,
                    Mockito.mock(ResultAggregator.class),
                    Mockito.mock(RuleEngine.class),
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

    @Test
    void detectConventionCandidates_returnsEmptyForNullOrBlankTree() {
        assertThat(AnalysisOrchestratorImpl.detectConventionCandidates(null)).isEmpty();
        assertThat(AnalysisOrchestratorImpl.detectConventionCandidates("")).isEmpty();
        assertThat(AnalysisOrchestratorImpl.detectConventionCandidates("   ")).isEmpty();
    }

    @Test
    void detectConventionCandidates_excludesKnownNonConventionFiles() {
        String tree = "./\n  README.md\n  CHANGELOG.md\n  LICENSE.md\n  AGENTS.md\n\nsrc/\n  Main.java\n";
        List<String> result = AnalysisOrchestratorImpl.detectConventionCandidates(tree);
        assertThat(result).containsExactly("AGENTS.md");
    }

    @Test
    void detectConventionCandidates_returnsAllWhenFewerThanThree() {
        String tree = "./\n  AGENTS.md\n  CONTRIBUTING.md\n\n";
        List<String> result = AnalysisOrchestratorImpl.detectConventionCandidates(tree);
        assertThat(result).containsExactly("AGENTS.md", "CONTRIBUTING.md");
    }

    @Test
    void detectConventionCandidates_sortsByPriorityThenLimitsToThree() {
        // 优先级：AGENTS.md(0) > CLAUDE.md(1) > CONTRIBUTING.md(5)；DEVELOPMENT.md(8)/CONVENTIONS.md(6) 被截断
        String tree = "./\n  CONTRIBUTING.md\n  CLAUDE.md\n  AGENTS.md\n  DEVELOPMENT.md\n  CONVENTIONS.md\n\n";
        List<String> result = AnalysisOrchestratorImpl.detectConventionCandidates(tree);
        assertThat(result).hasSize(3);
        assertThat(result.get(0)).isEqualTo("AGENTS.md");
        assertThat(result.get(1)).isEqualTo("CLAUDE.md");
        assertThat(result.get(2)).isEqualTo("CONTRIBUTING.md");
    }

    @Test
    void detectConventionCandidates_caseInsensitiveExclusion() {
        String tree = "./\n  readme.md\n  changelog.MD\n  AGENTS.md\n\n";
        List<String> result = AnalysisOrchestratorImpl.detectConventionCandidates(tree);
        assertThat(result).containsExactly("AGENTS.md");
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
