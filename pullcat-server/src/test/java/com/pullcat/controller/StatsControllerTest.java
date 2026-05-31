package com.pullcat.controller;

import com.pullcat.service.analysis.StatsService;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class StatsControllerTest {

    private final StatsService statsService = mock(StatsService.class);
    private final StatsController controller = new StatsController(statsService);

    @Test
    void getOverview() {
        Map<String, Object> expected = Map.of("totalReviews", 10, "totalIssues", 50);
        when(statsService.getOverview()).thenReturn(expected);

        ResponseEntity<Map<String, Object>> response = controller.getOverview();

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).containsEntry("totalReviews", 10);
    }

    @Test
    void getRepoStats() {
        Map<String, Object> expected = Map.of("totalReviews", 5, "repoFullName", "owner/repo");
        when(statsService.getRepoStats("owner", "repo")).thenReturn(expected);

        ResponseEntity<Map<String, Object>> response = controller.getRepoStats("owner", "repo");

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).containsEntry("totalReviews", 5);
    }
}
