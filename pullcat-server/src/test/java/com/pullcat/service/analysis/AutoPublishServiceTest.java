package com.pullcat.service.analysis;

import com.pullcat.dto.resp.AutoPublishRepoRespDTO;
import com.pullcat.dto.resp.BooleanStatusRespDTO;
import com.pullcat.service.AutoPublishService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AutoPublishServiceTest {

    @Mock
    ReviewRepository reviewRepository;

    @InjectMocks
    AutoPublishService autoPublishService;

    @Test
    void listAutoPublishRepos_returnsMappedDTOs() {
        when(reviewRepository.listAutoPublishRepos()).thenReturn(List.of("owner1/repo1", "owner2/repo2"));
        List<AutoPublishRepoRespDTO> result = autoPublishService.listAutoPublishRepos();
        assertEquals(2, result.size());
        assertEquals("owner1", result.get(0).getOwner());
        assertEquals("repo1", result.get(0).getRepo());
        assertTrue(result.get(0).isEnabled());
    }

    @Test
    void listAutoPublishRepos_empty_returnsEmptyList() {
        when(reviewRepository.listAutoPublishRepos()).thenReturn(List.of());
        assertTrue(autoPublishService.listAutoPublishRepos().isEmpty());
    }

    @Test
    void listAutoPublishRepos_invalidFormat_skipsEntry() {
        when(reviewRepository.listAutoPublishRepos()).thenReturn(List.of("invalid-no-slash", "owner/repo"));
        List<AutoPublishRepoRespDTO> result = autoPublishService.listAutoPublishRepos();
        assertEquals(1, result.size());
        assertEquals("owner", result.get(0).getOwner());
        assertEquals("repo", result.get(0).getRepo());
    }

    @Test
    void getStatus_delegatesToRepository() {
        when(reviewRepository.isAutoPublishEnabled("owner", "repo")).thenReturn(true);
        assertTrue(autoPublishService.getStatus("owner", "repo").isEnabled());
    }

    @Test
    void setEnabled_true_savesAndReturnsTrue() {
        BooleanStatusRespDTO result = autoPublishService.setEnabled("owner", "repo", Boolean.TRUE);
        verify(reviewRepository).setAutoPublishEnabled("owner", "repo", true);
        assertTrue(result.isEnabled());
    }

    @Test
    void setEnabled_false_savesAndReturnsFalse() {
        BooleanStatusRespDTO result = autoPublishService.setEnabled("owner", "repo", Boolean.FALSE);
        verify(reviewRepository).setAutoPublishEnabled("owner", "repo", false);
        assertFalse(result.isEnabled());
    }

    @Test
    void setEnabled_null_treatsAsFalse() {
        BooleanStatusRespDTO result = autoPublishService.setEnabled("owner", "repo", null);
        verify(reviewRepository).setAutoPublishEnabled("owner", "repo", false);
        assertFalse(result.isEnabled());
    }
}
