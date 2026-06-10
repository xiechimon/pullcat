package com.pullcat.service.analysis;

import com.pullcat.dao.entity.GitHubInstallationDO;
import com.pullcat.dao.entity.UserDO;
import com.pullcat.dao.mapper.GitHubInstallationMapper;
import com.pullcat.dao.mapper.UserMapper;
import com.pullcat.service.analysis.impl.GitHubInstallationServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GitHubInstallationServiceImplTest {

    @Mock
    GitHubInstallationMapper gitHubInstallationMapper;

    @Mock
    UserMapper userMapper;

    @InjectMocks
    GitHubInstallationServiceImpl gitHubInstallationService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(gitHubInstallationService, "baseMapper", gitHubInstallationMapper);
    }

    @Test
    void saveInstallation_insertsRecordAndBindsUserInstallationId() {
        UserDO user = new UserDO();
        user.setId("u1");
        user.setGithubLogin("xiechimon");
        when(gitHubInstallationMapper.selectById(42L)).thenReturn(null);
        when(userMapper.selectOne(any())).thenReturn(user);

        gitHubInstallationService.saveInstallation(42L, "xiechimon", "User");

        ArgumentCaptor<GitHubInstallationDO> installationCaptor = ArgumentCaptor.forClass(GitHubInstallationDO.class);
        verify(gitHubInstallationMapper).insert(installationCaptor.capture());
        assertEquals(42L, installationCaptor.getValue().getInstallationId());
        assertEquals("xiechimon", installationCaptor.getValue().getAccountLogin());
        assertEquals("User", installationCaptor.getValue().getAccountType());

        verify(userMapper).updateById(user);
        assertEquals(42L, user.getInstallationId());
    }

    @Test
    void suspendInstallation_updatesSuspendedAt() {
        GitHubInstallationDO record = new GitHubInstallationDO();
        record.setInstallationId(99L);
        when(gitHubInstallationMapper.selectById(99L)).thenReturn(record);

        gitHubInstallationService.suspendInstallation(99L);

        verify(gitHubInstallationMapper).updateById(record);
        assertTrue(record.getSuspendedAt() != null);
    }
}
