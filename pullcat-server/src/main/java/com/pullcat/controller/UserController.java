package com.pullcat.controller;

import com.pullcat.common.convention.result.Result;
import com.pullcat.common.convention.result.Results;
import com.pullcat.dto.resp.CurrentUserRespDTO;
import com.pullcat.dto.resp.LogoutRespDTO;
import com.pullcat.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 用户控制层
 */
@RestController
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/api/user")
    public Result<CurrentUserRespDTO> currentUser(@AuthenticationPrincipal OAuth2User principal) {
        return Results.success(userService.getCurrentUser(principal));
    }

    @PostMapping("/api/logout")
    public Result<LogoutRespDTO> logout(HttpServletRequest request) {
        return Results.success(userService.logout(request));
    }
}
