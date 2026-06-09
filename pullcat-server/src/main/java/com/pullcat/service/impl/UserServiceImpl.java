package com.pullcat.service.impl;

import com.pullcat.dao.entity.UserDO;
import com.pullcat.dto.resp.CurrentUserRespDTO;
import com.pullcat.dto.resp.LogoutRespDTO;
import com.pullcat.service.UserService;
import com.pullcat.service.analysis.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;

    @Override
    public CurrentUserRespDTO getCurrentUser(String login) {
        CurrentUserRespDTO response = new CurrentUserRespDTO();
        if (login == null || login.isBlank()) {
            response.setAuthenticated(false);
            return response;
        }

        UserDO user = userRepository.findByLogin(login);
        response.setAuthenticated(true);
        response.setLogin(login);
        if (user != null) {
            response.setAvatarUrl(user.getAvatarUrl());
            response.setName(user.getGithubLogin());
        }
        return response;
    }

    @Override
    public LogoutRespDTO logout(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        SecurityContextHolder.clearContext();
        return new LogoutRespDTO("logged_out");
    }
}
