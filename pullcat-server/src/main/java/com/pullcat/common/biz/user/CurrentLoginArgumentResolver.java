package com.pullcat.common.biz.user;

import org.springframework.core.MethodParameter;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Component;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.bind.support.WebDataBinderFactory;

/**
 * 解析当前登录用户的 GitHub login
 */
@Component
public class CurrentLoginArgumentResolver implements HandlerMethodArgumentResolver {

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(CurrentLogin.class)
                && String.class.equals(parameter.getParameterType());
    }

    @Override
    public Object resolveArgument(MethodParameter parameter,
                                  ModelAndViewContainer mavContainer,
                                  NativeWebRequest webRequest,
                                  WebDataBinderFactory binderFactory) {
        Object userPrincipal = webRequest.getUserPrincipal();
        if (userPrincipal instanceof Authentication) {
            Authentication authentication = (Authentication) userPrincipal;
            return resolveLogin(authentication);
        }

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null) {
            return resolveLogin(authentication);
        }
        return null;
    }

    private String resolveLogin(Authentication authentication) {
        Object principal = authentication.getPrincipal();
        if (principal instanceof OAuth2User oauthUser) {
            String login = oauthUser.getAttribute("login");
            return login != null ? login : authentication.getName();
        }
        if (authentication instanceof OAuth2AuthenticationToken oauth) {
            return oauth.getName();
        }
        return authentication.getName();
    }
}
