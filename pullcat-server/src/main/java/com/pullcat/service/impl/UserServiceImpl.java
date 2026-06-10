package com.pullcat.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.pullcat.common.constant.RedisKeys;
import com.pullcat.dao.entity.UserDO;
import com.pullcat.dao.mapper.UserMapper;
import com.pullcat.dto.resp.CurrentUserRespDTO;
import com.pullcat.dto.resp.LogoutRespDTO;
import com.pullcat.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserServiceImpl extends ServiceImpl<UserMapper, UserDO> implements UserService {

    private static final String ANONYMOUS_LOGIN = "anonymousUser";

    private final RedisTemplate<String, Object> redisTemplate;

    @Override
    public CurrentUserRespDTO getCurrentUser(String login) {
        CurrentUserRespDTO response = new CurrentUserRespDTO();
        if (login == null || login.isBlank() || ANONYMOUS_LOGIN.equals(login)) {
            response.setAuthenticated(false);
            return response;
        }

        UserDO user = findByLogin(login);
        response.setAuthenticated(true);
        response.setLogin(login);
        if (user != null) {
            response.setAvatarUrl(user.getAvatarUrl());
            response.setName(user.getGithubLogin());
            response.setHasInstallation(user.getInstallationId() != null);
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

    public UserDO findByLogin(String login) {
        Object cachedId = redisTemplate.opsForValue().get(RedisKeys.userLoginKey(login));
        if (cachedId != null) {
            Object cachedUser = redisTemplate.opsForValue().get(RedisKeys.userKey(cachedId.toString()));
            if (cachedUser instanceof UserDO userDO) {
                return userDO;
            }
        }

        UserDO user = baseMapper.selectOne(new LambdaQueryWrapper<UserDO>()
                .eq(UserDO::getGithubLogin, login));
        if (user != null) {
            redisTemplate.opsForValue().set(RedisKeys.userKey(user.getId()), user);
            redisTemplate.opsForValue().set(RedisKeys.userLoginKey(login), user.getId());
        }
        return user;
    }
}
