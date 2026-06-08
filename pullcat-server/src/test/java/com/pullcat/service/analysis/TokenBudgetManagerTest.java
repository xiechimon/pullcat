package com.pullcat.service.analysis;

import com.pullcat.dto.resp.FileContentRespDTO;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class TokenBudgetManagerTest {

    private final TokenBudgetManager manager = new TokenBudgetManager(1000);

    @Test
    void estimateTokens() {
        assertThat(manager.estimateTokens("12345678")).isEqualTo(2);
        assertThat(manager.estimateTokens("")).isEqualTo(0);
        assertThat(manager.estimateTokens(null)).isEqualTo(0);
    }

    @Test
    void isWithinBudget() {
        String shortText = "a".repeat(100);
        String longText = "a".repeat(5000);

        assertThat(manager.isWithinBudget(shortText)).isTrue();
        assertThat(manager.isWithinBudget(longText)).isFalse();
    }

    @Test
    void chunkFilesWithinBudget() {
        List<FileContentRespDTO> files = List.of(
                new FileContentRespDTO("a.java", "short", ""),
                new FileContentRespDTO("b.java", "also short", "")
        );

        List<List<FileContentRespDTO>> chunks = manager.chunkFiles("header", files);

        assertThat(chunks).hasSize(1);
        assertThat(chunks.get(0)).hasSize(2);
    }

    @Test
    void chunkFilesExceedsBudget() {
        String bigContent = "x".repeat(3000);

        List<FileContentRespDTO> files = List.of(
                new FileContentRespDTO("a.java", bigContent, ""),
                new FileContentRespDTO("b.java", bigContent, ""),
                new FileContentRespDTO("c.java", bigContent, "")
        );

        List<List<FileContentRespDTO>> chunks = manager.chunkFiles("header", files);

        assertThat(chunks).hasSize(3);
    }

    @Test
    void chunkFilesExcludesFlagged() {
        FileContentRespDTO excluded = new FileContentRespDTO("img.png", "binary", "");
        excluded.setExcluded(true);

        List<FileContentRespDTO> files = List.of(excluded, new FileContentRespDTO("a.java", "code", ""));

        List<List<FileContentRespDTO>> chunks = manager.chunkFiles("header", files);

        assertThat(chunks).hasSize(1);
        assertThat(chunks.get(0)).hasSize(1);
        assertThat(chunks.get(0).get(0).getPath()).isEqualTo("a.java");
    }

    @Test
    void chunkFilesEmpty() {
        List<List<FileContentRespDTO>> chunks = manager.chunkFiles("header", new ArrayList<>());
        assertThat(chunks).isEmpty();
    }
}
