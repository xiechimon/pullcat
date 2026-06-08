package com.pullcat.controller;

import com.pullcat.common.convention.result.Result;
import com.pullcat.common.convention.result.Results;
import com.pullcat.dao.entity.UserDO;
import com.pullcat.dto.resp.CurrentUserRespDTO;
import com.pullcat.dto.resp.LogoutRespDTO;
import com.pullcat.service.analysis.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class UserController {

    private final UserRepository userRepository;

    public UserController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @GetMapping("/api/user")
    public ResponseEntity<Result<CurrentUserRespDTO>> currentUser(@AuthenticationPrincipal OAuth2User principal) {
        if (principal == null) {
            CurrentUserRespDTO response = new CurrentUserRespDTO();
            response.setAuthenticated(false);
            return ResponseEntity.ok(Results.success(response));
        }
        String login = principal.getAttribute("login");
        UserDO user = userRepository.findByLogin(login);
        CurrentUserRespDTO response = new CurrentUserRespDTO();
        response.setAuthenticated(true);
        response.setLogin(login);
        response.setAvatarUrl(user != null ? user.getAvatarUrl() : principal.getAttribute("avatar_url"));
        response.setName(principal.getAttribute("name"));
        return ResponseEntity.ok(Results.success(response));
    }

    @PostMapping("/api/logout")
    public ResponseEntity<Result<LogoutRespDTO>> logout(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        SecurityContextHolder.clearContext();
        return ResponseEntity.ok(Results.success(new LogoutRespDTO("logged_out")));
    }
}
