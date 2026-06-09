package com.pullcat.service.analysis;

import com.pullcat.dao.entity.UserDO;
import com.pullcat.dto.resp.CurrentUserRespDTO;
import com.pullcat.dto.resp.LogoutRespDTO;
import com.pullcat.service.impl.UserServiceImpl;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.core.user.OAuth2User;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    UserRepository userRepository;

    @InjectMocks
    UserServiceImpl userService;

    @Test
    void getCurrentUser_nullPrincipal_returnsUnauthenticated() {
        CurrentUserRespDTO result = userService.getCurrentUser(null);

        assertFalse(result.isAuthenticated());
        assertNull(result.getLogin());
    }

    @Test
    void getCurrentUser_withPrincipal_returnsUserData() {
        OAuth2User principal = mock(OAuth2User.class);
        when(principal.getAttribute("login")).thenReturn("xiechimon");
        when(principal.getAttribute("name")).thenReturn("Xie Chi Mon");
        when(principal.getAttribute("avatar_url")).thenReturn("https://avatars.github.com/u/1");
        when(userRepository.findByLogin("xiechimon")).thenReturn(null);

        CurrentUserRespDTO result = userService.getCurrentUser(principal);

        assertTrue(result.isAuthenticated());
        assertEquals("xiechimon", result.getLogin());
        assertEquals("Xie Chi Mon", result.getName());
        assertEquals("https://avatars.github.com/u/1", result.getAvatarUrl());
    }

    @Test
    void getCurrentUser_withStoredUser_usesStoredAvatarUrl() {
        OAuth2User principal = mock(OAuth2User.class);
        when(principal.getAttribute("login")).thenReturn("xiechimon");
        when(principal.getAttribute("name")).thenReturn("Xie");
        UserDO user = new UserDO();
        user.setAvatarUrl("https://stored-avatar.com/img.png");
        when(userRepository.findByLogin("xiechimon")).thenReturn(user);

        CurrentUserRespDTO result = userService.getCurrentUser(principal);

        assertEquals("https://stored-avatar.com/img.png", result.getAvatarUrl());
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
