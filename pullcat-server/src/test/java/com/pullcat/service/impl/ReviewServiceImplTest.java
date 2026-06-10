package com.pullcat.service.impl;

import com.pullcat.common.enums.AnalysisStatus;
import com.pullcat.common.enums.AnalysisType;
import com.pullcat.dto.resp.AnalysisResultRespDTO;
import com.pullcat.service.analysis.AnalysisTaskFactory;
import com.pullcat.service.analysis.ContextBuilder;
import com.pullcat.service.analysis.PromptLoader;
import com.pullcat.service.analysis.ReviewSessionService;
import com.pullcat.service.analysis.impl.ReviewPublisher;
import com.pullcat.service.llm.AnalysisService;
import com.pullcat.service.llm.AnalysisTask;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ReviewServiceImplTest {

    @Test
    void executeTask_usesTaskTemplateNameInsteadOfAnalysisTypeTemplateName() {
        ExecutorService executorService = Executors.newSingleThreadExecutor();
        try {
            PromptLoader promptLoader = Mockito.mock(PromptLoader.class);
            when(promptLoader.loadTemplate("custom-template")).thenReturn("template body");
            when(promptLoader.populateTemplate("template body", Map.of("key", "value"))).thenReturn("final prompt");

            ReviewServiceImpl service = new ReviewServiceImpl(
                    Mockito.mock(com.pullcat.remote.GitHubApiService.class),
                    promptLoader,
                    Mockito.mock(ContextBuilder.class),
                    Mockito.mock(ReviewSessionService.class),
                    Mockito.mock(AnalysisTaskFactory.class),
                    executorService,
                    new SimpleMeterRegistry(),
                    Mockito.mock(ReviewPublisher.class)
            );

            AnalysisTask task = new StubAnalysisTask();

            AnalysisResultRespDTO result = (AnalysisResultRespDTO) ReflectionTestUtils.invokeMethod(
                    service,
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
