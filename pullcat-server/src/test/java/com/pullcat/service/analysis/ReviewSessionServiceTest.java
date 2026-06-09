package com.pullcat.service.analysis;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.pullcat.common.enums.SessionStatus;
import com.pullcat.dao.entity.RepoAutoPublishDO;
import com.pullcat.dao.entity.ReviewDO;
import com.pullcat.dao.mapper.RepoAutoPublishMapper;
import com.pullcat.dao.mapper.ReviewMapper;
import com.pullcat.dto.resp.ReviewSessionRespDTO;
import com.pullcat.service.analysis.impl.ReviewSessionServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReviewSessionServiceTest {

    @Mock
    ReviewMapper reviewMapper;

    @Mock
    RepoAutoPublishMapper repoAutoPublishMapper;

    private ReviewSessionService reviewSessionService;

    @BeforeEach
    void setUp() {
        reviewSessionService = new ReviewSessionServiceImpl(
                reviewMapper,
                repoAutoPublishMapper,
                new ObjectMapper().registerModule(new JavaTimeModule())
        );
    }

    @Test
    void save_newSession_insertsSnapshot() {
        ReviewSessionRespDTO session = review("r1", "owner/repo", "xmon");
        when(reviewMapper.selectById("r1")).thenReturn(null);

        reviewSessionService.save(session);

        ArgumentCaptor<ReviewDO> captor = ArgumentCaptor.forClass(ReviewDO.class);
        verify(reviewMapper).insert(captor.capture());
        ReviewDO saved = captor.getValue();
        assertEquals("r1", saved.getId());
        assertEquals("owner/repo", saved.getRepositoryFullName());
        assertEquals("xmon", saved.getUserId());
        assertEquals(SessionStatus.FETCHING.name(), saved.getStatus());
        assertNotNull(saved.getSnapshotJson());
        assertNotNull(saved.getUpdatedAt());
    }

    @Test
    void findById_readsSnapshotFromDatabase() {
        ReviewDO reviewDO = new ReviewDO();
        reviewDO.setId("r1");
        reviewDO.setSnapshotJson("{\"id\":\"r1\",\"prUrl\":\"https://github.com/o/r/pull/1\",\"status\":\"COMPLETED\"}");
        when(reviewMapper.selectById("r1")).thenReturn(reviewDO);

        ReviewSessionRespDTO result = reviewSessionService.findById("r1");

        assertEquals("r1", result.getId());
        assertEquals("https://github.com/o/r/pull/1", result.getPrUrl());
        assertEquals(SessionStatus.COMPLETED, result.getStatus());
    }

    @Test
    void listAutoPublishRepos_returnsEnabledFullNames() {
        RepoAutoPublishDO config = new RepoAutoPublishDO("owner", "repo", true);
        when(repoAutoPublishMapper.selectList(any())).thenReturn(List.of(config));

        List<String> result = reviewSessionService.listAutoPublishRepos();

        assertEquals(List.of("owner/repo"), result);
    }

    @Test
    void isAutoPublishEnabled_returnsFlagFromDatabase() {
        when(repoAutoPublishMapper.selectById("owner/repo")).thenReturn(new RepoAutoPublishDO("owner", "repo", true));

        assertTrue(reviewSessionService.isAutoPublishEnabled("owner", "repo"));
        assertFalse(reviewSessionService.isAutoPublishEnabled("owner", "missing"));
    }

    @Test
    void setAutoPublishEnabled_true_insertsWhenMissing() {
        when(repoAutoPublishMapper.selectById("owner/repo")).thenReturn(null);

        reviewSessionService.setAutoPublishEnabled("owner", "repo", true);

        verify(repoAutoPublishMapper).insert(any(RepoAutoPublishDO.class));
        verify(repoAutoPublishMapper, never()).deleteById(anyString());
    }

    @Test
    void setAutoPublishEnabled_false_deletesConfig() {
        reviewSessionService.setAutoPublishEnabled("owner", "repo", false);

        verify(repoAutoPublishMapper).deleteById("owner/repo");
    }

    private ReviewSessionRespDTO review(String id, String repo, String login) {
        ReviewSessionRespDTO session = new ReviewSessionRespDTO();
        session.setId(id);
        session.setPrUrl("https://github.com/o/r/pull/1");
        session.setRepositoryFullName(repo);
        session.setUserId(login);
        session.setStatus(SessionStatus.FETCHING);
        session.setCreatedAt(Instant.parse("2026-06-09T10:00:00Z"));
        return session;
    }
}
