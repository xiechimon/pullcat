package com.pullcat.config;

import com.pullcat.service.github.GitHubForbiddenException;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.util.retry.Retry;

import java.time.Duration;

public final class RetryConfig {

    private RetryConfig() {}

    public static Retry githubRetry() {
        return Retry.backoff(3, Duration.ofSeconds(1))
                .maxBackoff(Duration.ofSeconds(4))
                .filter(throwable -> !isNonRetryable(throwable));
    }

    public static Retry llmRetry() {
        return Retry.backoff(2, Duration.ofSeconds(3))
                .maxBackoff(Duration.ofSeconds(9))
                .filter(throwable -> !isNonRetryable(throwable));
    }

    private static boolean isNonRetryable(Throwable throwable) {
        if (throwable instanceof WebClientResponseException ex) {
            return ex.getStatusCode().is4xxClientError();
        }
        if (throwable instanceof GitHubForbiddenException) {
            return true;
        }
        return false;
    }
}

