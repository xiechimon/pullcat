package com.pullcat.controller;

import com.pullcat.common.biz.user.CurrentLogin;
import com.pullcat.common.convention.result.Result;
import com.pullcat.common.convention.result.Results;
import com.pullcat.dto.resp.CurrentUserRespDTO;
import com.pullcat.dto.resp.LogoutRespDTO;
import com.pullcat.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 用户控制层
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/pullcat/v1")
public class UserController {

    private final UserService userService;

    @GetMapping("/user")
    public Result<CurrentUserRespDTO> currentUser(@CurrentLogin String login) {
        return Results.success(userService.getCurrentUser(login));
    }

    /**
     * 注销登录
     */
    @PostMapping("/logout")
    public Result<LogoutRespDTO> logout(HttpServletRequest request) {
        return Results.success(userService.logout(request));
    }
}
