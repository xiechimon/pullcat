package com.pullcat.service.analysis;

import com.pullcat.common.enums.AnalysisType;
import com.pullcat.service.analysis.impl.AnalysisTaskFactoryImpl;
import com.pullcat.service.llm.AnalysisTask;
import com.pullcat.service.llm.impl.AggregationAnalysisServiceImpl;
import com.pullcat.service.llm.impl.ConsistencyAnalysisServiceImpl;
import com.pullcat.service.llm.impl.QualityAnalysisServiceImpl;
import com.pullcat.service.llm.impl.RiskAnalysisServiceImpl;
import com.pullcat.service.llm.impl.SummaryAnalysisServiceImpl;
import com.pullcat.service.llm.impl.TestingGapAnalysisServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class AnalysisTaskFactoryImplTest {

    private AnalysisTaskFactoryImpl factory;

    @BeforeEach
    void setUp() {
        factory = new AnalysisTaskFactoryImpl(
                Mockito.mock(ChatClient.class),
                Mockito.mock(ChatClient.class)
        );
        ReflectionTestUtils.setField(factory, "lightModelName", "light-model");
        ReflectionTestUtils.setField(factory, "heavyModelName", "heavy-model");
    }

    @Test
    void create_summary_returnsSummaryTask() {
        AnalysisTask task = factory.create(AnalysisType.SUMMARY);
        assertInstanceOf(SummaryAnalysisServiceImpl.class, task);
        assertEquals(AnalysisType.SUMMARY, task.getType());
        assertEquals("summary", task.getTemplateName());
    }

    @Test
    void create_risk_returnsRiskTask() {
        AnalysisTask task = factory.create(AnalysisType.RISK);
        assertInstanceOf(RiskAnalysisServiceImpl.class, task);
        assertEquals(AnalysisType.RISK, task.getType());
        assertEquals("risk", task.getTemplateName());
    }

    @Test
    void create_quality_returnsQualityTask() {
        AnalysisTask task = factory.create(AnalysisType.QUALITY);
        assertInstanceOf(QualityAnalysisServiceImpl.class, task);
        assertEquals(AnalysisType.QUALITY, task.getType());
        assertEquals("quality", task.getTemplateName());
    }

    @Test
    void create_consistency_returnsConsistencyTask() {
        AnalysisTask task = factory.create(AnalysisType.CONSISTENCY);
        assertInstanceOf(ConsistencyAnalysisServiceImpl.class, task);
        assertEquals(AnalysisType.CONSISTENCY, task.getType());
        assertEquals("consistency", task.getTemplateName());
    }

    @Test
    void create_testing_returnsTestingTask() {
        AnalysisTask task = factory.create(AnalysisType.TESTING);
        assertInstanceOf(TestingGapAnalysisServiceImpl.class, task);
        assertEquals(AnalysisType.TESTING, task.getType());
        assertEquals("testing", task.getTemplateName());
    }

    @Test
    void create_aggregation_returnsAggregationTask() {
        AnalysisTask task = factory.create(AnalysisType.AGGREGATION);
        assertInstanceOf(AggregationAnalysisServiceImpl.class, task);
        assertEquals(AnalysisType.AGGREGATION, task.getType());
        assertEquals("aggregation", task.getTemplateName());
    }
}
