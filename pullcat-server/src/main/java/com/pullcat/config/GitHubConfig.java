package com.pullcat.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "pullcat.github")
public class GitHubConfig {

    private String token;

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }
}
