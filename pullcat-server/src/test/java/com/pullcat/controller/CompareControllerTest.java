package com.pullcat.controller;

import com.pullcat.service.analysis.CompareService;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CompareControllerTest {

    private final CompareService compareService = mock(CompareService.class);
    private final CompareController controller = new CompareController(compareService);

    @Test
    void compareWithTwoIds() {
        Map<String, Object> expected = Map.of("newCount", 3, "fixedCount", 1);
        when(compareService.compare("r1", "r2")).thenReturn(expected);

        ResponseEntity<Map<String, Object>> response = controller.compare(
                Map.of("reviewIds", List.of("r1", "r2")));

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).containsEntry("newCount", 3);
    }

    @Test
    void compareWithInvalidInput() {
        ResponseEntity<Map<String, Object>> response = controller.compare(
                Map.of("reviewIds", List.of("only-one")));

        assertThat(response.getStatusCode().value()).isEqualTo(400);
        assertThat(response.getBody()).containsKey("error");
    }
}
