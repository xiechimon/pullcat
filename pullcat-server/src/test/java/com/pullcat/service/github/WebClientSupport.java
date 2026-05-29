package com.pullcat.service.github;

import org.springframework.web.reactive.function.client.WebClient;

class WebClientSupport {
    static WebClient noopClient() {
        return WebClient.builder()
                .exchangeFunction(req ->
                        reactor.core.publisher.Mono.error(
                                new UnsupportedOperationException("not used in unit test")))
                .build();
    }
}
