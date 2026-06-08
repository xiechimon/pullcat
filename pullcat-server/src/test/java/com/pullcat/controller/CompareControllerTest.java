package com.pullcat.controller;

import com.pullcat.common.convention.exception.ClientException;
import com.pullcat.common.convention.result.Result;
import com.pullcat.dto.req.CompareReviewsReqDTO;
import com.pullcat.dto.resp.CompareReviewsRespDTO;
import com.pullcat.service.analysis.CompareService;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CompareControllerTest {

    private final CompareService compareService = mock(CompareService.class);
    private final CompareController controller = new CompareController(compareService);

    @Test
    void compareWithTwoIds() {
        CompareReviewsRespDTO expected = new CompareReviewsRespDTO();
        expected.setNewCount(3);
        expected.setFixedCount(1);
        when(compareService.compare("r1", "r2")).thenReturn(expected);

        CompareReviewsReqDTO requestParam = new CompareReviewsReqDTO();
        requestParam.setReviewIds(List.of("r1", "r2"));

        ResponseEntity<Result<CompareReviewsRespDTO>> response = controller.compare(
                requestParam);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isSuccess()).isTrue();
        assertThat(response.getBody().getData().getNewCount()).isEqualTo(3);
    }

    @Test
    void compareWithInvalidInput() {
        CompareReviewsReqDTO requestParam = new CompareReviewsReqDTO();
        requestParam.setReviewIds(List.of("only-one"));

        assertThat(controller.compare(requestParam))
                .isInstanceOf(ClientException.class);
    }
}
