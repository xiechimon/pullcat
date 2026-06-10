package com.pullcat.config.security;

import com.pullcat.common.constant.RedisKeys;
import com.pullcat.dao.entity.UserDO;
import com.pullcat.dao.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Component;


@Component
@RequiredArgsConstructor
public class CustomOAuth2UserService implements OAuth2UserService<OAuth2UserRequest, OAuth2User> {

    private final UserMapper userMapper;
    private final RedisTemplate<String, Object> redisTemplate;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = new DefaultOAuth2UserService().loadUser(userRequest);

        String login = oAuth2User.getAttribute("login");
        Integer githubId = oAuth2User.getAttribute("id");
        String avatarUrl = oAuth2User.getAttribute("avatar_url");
        String email = oAuth2User.getAttribute("email");
        String name = oAuth2User.getAttribute("name");

        if (login != null && githubId != null) {
            String id = String.valueOf(githubId);
            UserDO user = userMapper.selectById(id);
            boolean isNew = user == null;
            if (isNew) {
                user = new UserDO();
                user.setId(id);
            }
            user.setGithubLogin(login);
            user.setGithubId(githubId.longValue());
            user.setAvatarUrl(avatarUrl);
            user.setEmail(email);
            user.setName(name);
            if (isNew) {
                userMapper.insert(user);
            } else {
                userMapper.updateById(user);
            }
            redisTemplate.opsForValue().set(RedisKeys.userKey(id), user);
            redisTemplate.opsForValue().set(RedisKeys.userLoginKey(login), id);
        }

        return oAuth2User;
    }
}
