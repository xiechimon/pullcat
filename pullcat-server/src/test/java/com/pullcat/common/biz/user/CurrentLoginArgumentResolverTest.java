package com.pullcat.common.biz.user;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.context.request.NativeWebRequest;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CurrentLoginArgumentResolverTest {

    private final CurrentLoginArgumentResolver resolver = new CurrentLoginArgumentResolver();

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void resolveArgument_withAnonymousAuthentication_returnsNull() throws Exception {
        NativeWebRequest webRequest = mock(NativeWebRequest.class);
        AnonymousAuthenticationToken authentication = new AnonymousAuthenticationToken(
                "key",
                "anonymousUser",
                AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS")
        );
        when(webRequest.getUserPrincipal()).thenReturn(authentication);

        Object result = resolver.resolveArgument(currentLoginParameter(), null, webRequest, null);

        assertNull(result);
    }

    @Test
    void resolveArgument_withAuthenticatedUser_returnsLogin() throws Exception {
        NativeWebRequest webRequest = mock(NativeWebRequest.class);
        TestingAuthenticationToken authentication = new TestingAuthenticationToken("xiechimon", null);
        authentication.setAuthenticated(true);
        when(webRequest.getUserPrincipal()).thenReturn(authentication);

        Object result = resolver.resolveArgument(currentLoginParameter(), null, webRequest, null);

        assertEquals("xiechimon", result);
    }

    private MethodParameter currentLoginParameter() throws NoSuchMethodException {
        Method method = TestController.class.getDeclaredMethod("handle", String.class);
        return new MethodParameter(method, 0);
    }

    private static class TestController {
        @SuppressWarnings("unused")
        void handle(@CurrentLogin String login) {
        }
    }
}
