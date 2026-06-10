package com.pullcat.remote.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pullcat.common.constant.RedisKeys;
import com.pullcat.config.infra.GitHubConfig;
import com.pullcat.remote.GitHubInstallationTokenService;
import io.jsonwebtoken.Jwts;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;

/**
 * GitHub App Installation Token 服务实现
 */
@Slf4j
@Service
public class GitHubInstallationTokenServiceImpl implements GitHubInstallationTokenService {

    private static final Duration TOKEN_TTL = Duration.ofMinutes(58);

    private final GitHubConfig config;
    private final RedisTemplate<String, Object> redisTemplate;
    private final WebClient webClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    public GitHubInstallationTokenServiceImpl(GitHubConfig config,
                                              RedisTemplate<String, Object> redisTemplate) {
        this.config = config;
        this.redisTemplate = redisTemplate;
        this.webClient = WebClient.builder()
                .baseUrl("https://api.github.com")
                .defaultHeader("Accept", "application/vnd.github+json")
                .defaultHeader("User-Agent", "pullcat")
                .build();
    }

    public GitHubInstallationTokenServiceImpl(GitHubConfig config,
                                       RedisTemplate<String, Object> redisTemplate,
                                       WebClient webClient) {
        this.config = config;
        this.redisTemplate = redisTemplate;
        this.webClient = webClient;
    }

    @Override
    public Mono<String> getInstallationToken(long installationId) {
        String cacheKey = RedisKeys.installationTokenKey(installationId);
        Object cached = redisTemplate.opsForValue().get(cacheKey);
        if (cached instanceof String token) {
            return Mono.just(token);
        }
        return fetchFreshToken(installationId)
                .doOnNext(token -> redisTemplate.opsForValue().set(cacheKey, token, TOKEN_TTL));
    }

    private Mono<String> fetchFreshToken(long installationId) {
        String jwt;
        try {
            jwt = generateAppJwt();
        } catch (Exception e) {
            return Mono.error(new IllegalStateException("Failed to generate App JWT: " + e.getMessage(), e));
        }
        return webClient.post()
                .uri("/app/installations/{id}/access_tokens", installationId)
                .header("Authorization", "Bearer " + jwt)
                .retrieve()
                .bodyToMono(String.class)
                .map(this::parseToken);
    }

    public String generateAppJwt() {
        if (config.getAppId() == null || config.getPrivateKey() == null || config.getPrivateKey().isBlank()) {
            throw new IllegalStateException("GitHub App ID or private key not configured");
        }
        PrivateKey privateKey = loadPrivateKey(config.getPrivateKey());
        Instant now = Instant.now();
        return Jwts.builder()
                .issuer(String.valueOf(config.getAppId()))
                .issuedAt(Date.from(now.minusSeconds(60)))   // 回退 60s 防时钟偏差
                .expiration(Date.from(now.plusSeconds(600))) // 10 分钟有效期
                .signWith(privateKey, Jwts.SIG.RS256)
                .compact();
    }

    private PrivateKey loadPrivateKey(String pem) {
        // 移除 PEM 头尾行与空白，期望 PKCS#8 格式
        // 转换命令：openssl pkcs8 -topk8 -inform PEM -outform PEM -nocrypt -in key.pem -out key-pkcs8.pem
        String stripped = pem
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replaceAll("\\s", "");
        byte[] keyBytes = Base64.getDecoder().decode(stripped);
        try {
            KeyFactory kf = KeyFactory.getInstance("RSA");
            return kf.generatePrivate(new PKCS8EncodedKeySpec(keyBytes));
        } catch (Exception e) {
            throw new IllegalStateException("Failed to load RSA private key", e);
        }
    }

    private String parseToken(String json) {
        try {
            JsonNode node = objectMapper.readTree(json);
            return node.get("token").asText();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to parse installation token response", e);
        }
    }
}
