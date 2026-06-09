package com.pullcat.service.analysis;

import com.pullcat.common.convention.exception.ClientException;
import com.pullcat.dao.entity.RepoDO;
import com.pullcat.dto.req.CreateRepoReqDTO;
import com.pullcat.dto.resp.DeletedRespDTO;
import com.pullcat.dto.resp.RepoRespDTO;
import com.pullcat.service.RepoService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RepoServiceTest {

    @Mock
    RepoRepository repoRepository;

    @InjectMocks
    RepoService repoService;

    @Test
    void listRepos_returnsMappedDTOs() {
        RepoDO repo = new RepoDO("owner", "repo");
        when(repoRepository.findAll()).thenReturn(List.of(repo));

        List<RepoRespDTO> result = repoService.listRepos();

        assertEquals(1, result.size());
        assertEquals("owner", result.get(0).getOwner());
        assertEquals("repo", result.get(0).getRepo());
    }

    @Test
    void addRepo_nullOwner_throwsClientException() {
        CreateRepoReqDTO req = new CreateRepoReqDTO();
        req.setRepo("repo");

        assertThrows(ClientException.class, () -> repoService.addRepo(req));
    }

    @Test
    void addRepo_nullRepo_throwsClientException() {
        CreateRepoReqDTO req = new CreateRepoReqDTO();
        req.setOwner("owner");

        assertThrows(ClientException.class, () -> repoService.addRepo(req));
    }

    @Test
    void addRepo_valid_savesAndReturnsDTO() {
        CreateRepoReqDTO req = new CreateRepoReqDTO();
        req.setOwner("owner");
        req.setRepo("repo");
        req.setDescription("desc");

        RepoRespDTO result = repoService.addRepo(req);

        verify(repoRepository).save(any(RepoDO.class));
        assertEquals("owner", result.getOwner());
        assertEquals("repo", result.getRepo());
        assertEquals("desc", result.getDescription());
    }

    @Test
    void removeRepo_notFound_throwsClientException() {
        when(repoRepository.exists("owner", "repo")).thenReturn(false);

        assertThrows(ClientException.class, () -> repoService.removeRepo("owner", "repo"));
    }

    @Test
    void removeRepo_found_deletesAndReturnsDeleted() {
        when(repoRepository.exists("owner", "repo")).thenReturn(true);

        DeletedRespDTO result = repoService.removeRepo("owner", "repo");

        verify(repoRepository).delete("owner", "repo");
        assertTrue(result.isDeleted());
    }

    @Test
    void getRepo_notFound_throwsClientException() {
        when(repoRepository.findById("owner/repo")).thenReturn(null);

        assertThrows(ClientException.class, () -> repoService.getRepo("owner", "repo"));
    }

    @Test
    void getRepo_found_returnsDTO() {
        RepoDO repo = new RepoDO("owner", "repo");
        when(repoRepository.findById("owner/repo")).thenReturn(repo);

        RepoRespDTO result = repoService.getRepo("owner", "repo");

        assertEquals("owner", result.getOwner());
        assertEquals("owner/repo", result.getFullName());
    }
}
