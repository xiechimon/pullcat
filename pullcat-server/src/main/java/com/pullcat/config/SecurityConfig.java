package com.pullcat.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    /**
     * 开发环境：放行所有请求，启用 OAuth2 但不强制登录。
     * 部署上线时改 profile=prod。
     */
    @Bean
    @Profile("!prod")
    public SecurityFilterChain devFilterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
                .csrf(AbstractHttpConfigurer::disable)
                .oauth2Login(oauth -> oauth.defaultSuccessUrl("http://localhost:5173/dashboard", true));
        return http.build();
    }
}
