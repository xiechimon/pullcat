package com.pullcat.config.infra;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * GitHub 配置类，绑定 {@code pullcat.github} 前缀的配置项
 */
@Configuration
@ConfigurationProperties(prefix = "pullcat.github")
public class GitHubConfig {

    private String token;

    /**
     * GitHub App ID
     */
    private Long appId;

    /**
     * GitHub App 私钥（PKCS#8 PEM 格式）
     */
    private String privateKey;

    /**
     * GitHub App Webhook Secret
     */
    private String webhookSecret;

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }

    public Long getAppId() { return appId; }
    public void setAppId(Long appId) { this.appId = appId; }

    public String getPrivateKey() { return privateKey; }
    public void setPrivateKey(String privateKey) { this.privateKey = privateKey; }

    public String getWebhookSecret() { return webhookSecret; }
    public void setWebhookSecret(String webhookSecret) { this.webhookSecret = webhookSecret; }
}
