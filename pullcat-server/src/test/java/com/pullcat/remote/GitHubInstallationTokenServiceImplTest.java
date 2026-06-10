package com.pullcat.remote;

import com.pullcat.common.constant.RedisKeys;
import com.pullcat.config.infra.GitHubConfig;
import com.pullcat.remote.GitHubApiService;
import com.pullcat.remote.GitHubInstallationTokenService;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import com.pullcat.remote.impl.GitHubInstallationTokenServiceImpl;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Duration;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class GitHubInstallationTokenServiceImplTest {

    @Test
    @SuppressWarnings("unchecked")
    void getInstallationToken_returnsCachedTokenWhenPresent() {
        RedisTemplate<String, Object> redisTemplate = Mockito.mock(RedisTemplate.class);
        ValueOperations<String, Object> valueOps = Mockito.mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.get(RedisKeys.installationTokenKey(42L))).thenReturn("cached-token");

        GitHubConfig config = new GitHubConfig();
        WebClient webClient = Mockito.mock(WebClient.class, Mockito.RETURNS_DEEP_STUBS);

        GitHubInstallationTokenServiceImpl service =
                new GitHubInstallationTokenServiceImpl(config, redisTemplate, webClient);

        StepVerifier.create(service.getInstallationToken(42L))
                .expectNext("cached-token")
                .verifyComplete();

        // 命中缓存，不调用 webClient
        verifyNoInteractions(webClient);
    }

    @Test
    @SuppressWarnings("unchecked")
    void getInstallationToken_fetchesAndCachesWhenMiss() {
        RedisTemplate<String, Object> redisTemplate = Mockito.mock(RedisTemplate.class);
        ValueOperations<String, Object> valueOps = Mockito.mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.get(RedisKeys.installationTokenKey(99L))).thenReturn(null);

        GitHubConfig config = new GitHubConfig();
        config.setAppId(1L);
        config.setPrivateKey("");

        // 构造返回 {"token":"fresh-token"} 的 stub WebClient
        WebClient.RequestBodyUriSpec requestSpec = Mockito.mock(WebClient.RequestBodyUriSpec.class);
        WebClient.RequestBodySpec bodySpec = Mockito.mock(WebClient.RequestBodySpec.class);
        WebClient.ResponseSpec responseSpec = Mockito.mock(WebClient.ResponseSpec.class);
        WebClient webClient = Mockito.mock(WebClient.class);

        when(webClient.post()).thenReturn(requestSpec);
        when(requestSpec.uri(anyString(), eq(99L))).thenReturn(bodySpec);
        when(bodySpec.header(anyString(), anyString())).thenReturn(bodySpec);
        when(bodySpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(String.class)).thenReturn(Mono.just("{\"token\":\"fresh-token\"}"));

        GitHubInstallationTokenServiceImpl service =
                new GitHubInstallationTokenServiceImpl(config, redisTemplate, webClient);

        // spy 绕过 JWT 生成，测试缓存写入逻辑
        GitHubInstallationTokenServiceImpl spy = Mockito.spy(service);
        doReturn("stub-jwt").when(spy).generateAppJwt();

        StepVerifier.create(spy.getInstallationToken(99L))
                .expectNext("fresh-token")
                .verifyComplete();

        verify(valueOps).set(eq(RedisKeys.installationTokenKey(99L)),
                eq("fresh-token"), eq(Duration.ofMinutes(58)));
    }

    @Test
    void withInstallationToken_returnsIndependentApiServiceInstance() {
        GitHubConfig config = new GitHubConfig();
        GitHubInstallationTokenService tokenService = Mockito.mock(GitHubInstallationTokenService.class);
        when(tokenService.getInstallationToken(77L)).thenReturn(Mono.just("installation-token"));

        GitHubApiService service = new com.pullcat.remote.impl.GitHubApiServiceImpl(
                config,
                new SimpleMeterRegistry(),
                Mockito.mock(OAuth2AuthorizedClientService.class),
                tokenService
        );

        StepVerifier.create(service.withInstallationToken(77L))
                .assertNext(api -> {
                    org.junit.jupiter.api.Assertions.assertNotSame(service, api);
                    org.junit.jupiter.api.Assertions.assertInstanceOf(GitHubApiService.class, api);
                })
                .verifyComplete();
    }
}
