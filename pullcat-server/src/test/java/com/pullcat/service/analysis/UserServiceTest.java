package com.pullcat.service.analysis;

import com.pullcat.dao.entity.UserDO;
import com.pullcat.dao.mapper.UserMapper;
import com.pullcat.dto.resp.CurrentUserRespDTO;
import com.pullcat.dto.resp.LogoutRespDTO;
import com.pullcat.service.impl.UserServiceImpl;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    UserMapper userMapper;

    @Mock
    RedisTemplate<String, Object> redisTemplate;

    @Mock
    ValueOperations<String, Object> valueOperations;

    @InjectMocks
    UserServiceImpl userService;

    @BeforeEach
    void setUp() {
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        ReflectionTestUtils.setField(userService, "baseMapper", userMapper);
    }

    @Test
    void getCurrentUser_nullLogin_returnsUnauthenticated() {
        CurrentUserRespDTO result = userService.getCurrentUser(null);

        assertFalse(result.isAuthenticated());
        assertNull(result.getLogin());
    }

    @Test
    void getCurrentUser_withLogin_returnsAuthenticatedUser() {
        when(valueOperations.get(anyString())).thenReturn(null);
        when(userMapper.selectOne(any())).thenReturn(null);

        CurrentUserRespDTO result = userService.getCurrentUser("xiechimon");

        assertTrue(result.isAuthenticated());
        assertEquals("xiechimon", result.getLogin());
        assertNull(result.getName());
        assertNull(result.getAvatarUrl());
    }

    @Test
    void getCurrentUser_anonymousLogin_returnsUnauthenticated() {
        CurrentUserRespDTO result = userService.getCurrentUser("anonymousUser");

        assertFalse(result.isAuthenticated());
        assertNull(result.getLogin());
        verifyNoInteractions(userMapper);
    }

    @Test
    void getCurrentUser_withStoredUser_usesStoredAvatarUrl() {
        UserDO user = new UserDO();
        user.setGithubLogin("xiechimon");
        user.setAvatarUrl("https://stored-avatar.com/img.png");
        when(valueOperations.get(anyString())).thenReturn(null);
        when(userMapper.selectOne(any())).thenReturn(user);

        CurrentUserRespDTO result = userService.getCurrentUser("xiechimon");

        assertEquals("https://stored-avatar.com/img.png", result.getAvatarUrl());
        assertEquals("xiechimon", result.getName());
    }

    @Test
    void getCurrentUser_withInstallation_setsHasInstallationTrue() {
        UserDO user = new UserDO();
        user.setGithubLogin("xiechimon");
        user.setInstallationId(42L);
        when(valueOperations.get(anyString())).thenReturn(null);
        when(userMapper.selectOne(any())).thenReturn(user);

        CurrentUserRespDTO result = userService.getCurrentUser("xiechimon");

        assertTrue(result.isHasInstallation());
    }

    @Test
    void logout_withSession_invalidatesSession() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpSession session = mock(HttpSession.class);
        when(request.getSession(false)).thenReturn(session);

        LogoutRespDTO result = userService.logout(request);

        verify(session).invalidate();
        assertEquals("logged_out", result.getStatus());
    }

    @Test
    void logout_noSession_returnsLoggedOut() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getSession(false)).thenReturn(null);

        LogoutRespDTO result = userService.logout(request);

        assertEquals("logged_out", result.getStatus());
    }
}
