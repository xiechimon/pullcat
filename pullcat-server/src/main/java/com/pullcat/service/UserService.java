package com.pullcat.service;

import com.pullcat.dto.resp.CurrentUserRespDTO;
import com.pullcat.dto.resp.LogoutRespDTO;
import jakarta.servlet.http.HttpServletRequest;

/**
 * 用户业务服务
 */
public interface UserService {

    /**
     * 获取当前登录用户信息
     */
    CurrentUserRespDTO getCurrentUser(String login);

    /**
     * 注销登录，清除 Session 与安全上下文
     */
    LogoutRespDTO logout(HttpServletRequest request);
}
