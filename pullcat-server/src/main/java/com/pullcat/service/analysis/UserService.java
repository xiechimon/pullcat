package com.pullcat.service.analysis;

import com.pullcat.dao.entity.UserDO;
import com.pullcat.dto.resp.CurrentUserRespDTO;
import com.pullcat.dto.resp.LogoutRespDTO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

/**
 * 用户业务服务
 */
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    /**
     * 获取当前登录用户信息
     */
    public CurrentUserRespDTO getCurrentUser(OAuth2User principal) {
        CurrentUserRespDTO response = new CurrentUserRespDTO();
        if (principal == null) {
            response.setAuthenticated(false);
            return response;
        }

        String login = principal.getAttribute("login");
        UserDO user = userRepository.findByLogin(login);
        response.setAuthenticated(true);
        response.setLogin(login);
        response.setAvatarUrl(user != null ? user.getAvatarUrl() : principal.getAttribute("avatar_url"));
        response.setName(principal.getAttribute("name"));
        return response;
    }

    /**
     * 注销登录，清除 Session 与安全上下文
     */
    public LogoutRespDTO logout(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        SecurityContextHolder.clearContext();
        return new LogoutRespDTO("logged_out");
    }
}
