package com.pullcat.controller;

import com.pullcat.common.convention.result.Result;
import com.pullcat.dto.resp.RepoStatsRespDTO;
import com.pullcat.dto.resp.StatsOverviewRespDTO;
import com.pullcat.service.analysis.StatsService;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class StatsControllerTest {

    private final StatsService statsService = mock(StatsService.class);
    private final StatsController controller = new StatsController(statsService);

    @Test
    void getOverview() {
        StatsOverviewRespDTO expected = new StatsOverviewRespDTO();
        expected.setTotalReviews(10);
        expected.setTotalIssues(50);
        when(statsService.getOverview()).thenReturn(expected);

        ResponseEntity<Result<StatsOverviewRespDTO>> response = controller.getOverview();

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isSuccess()).isTrue();
        assertThat(response.getBody().getData().getTotalReviews()).isEqualTo(10);
    }

    @Test
    void getRepoStats() {
        RepoStatsRespDTO expected = new RepoStatsRespDTO();
        expected.setTotalReviews(5);
        expected.setRepoFullName("owner/repo");
        when(statsService.getRepoStats("owner", "repo")).thenReturn(expected);

        ResponseEntity<Result<RepoStatsRespDTO>> response = controller.getRepoStats("owner", "repo");

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isSuccess()).isTrue();
        assertThat(response.getBody().getData().getTotalReviews()).isEqualTo(5);
    }
}
