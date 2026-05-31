package com.pullcat.config;

import com.pullcat.service.analysis.RateLimiter;
import com.pullcat.service.analysis.RateLimiterInterceptor;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.time.Duration;

@Configuration
@Profile("prod")
public class WebConfig implements WebMvcConfigurer {

    @Value("${pullcat.rate-limit.default-max:60}")
    private int defaultMax;

    @Value("${pullcat.rate-limit.default-window-seconds:60}")
    private int defaultWindowSeconds;

    @Value("${pullcat.rate-limit.review-create-max:10}")
    private int reviewCreateMax;

    @Value("${pullcat.rate-limit.review-create-window-seconds:60}")
    private int reviewCreateWindowSeconds;

    @Value("${pullcat.rate-limit.stats-max:30}")
    private int statsMax;

    @Value("${pullcat.rate-limit.stats-window-seconds:60}")
    private int statsWindowSeconds;

    private final RateLimiterInterceptor rateLimiterInterceptor;

    public WebConfig(RateLimiter rateLimiter, MeterRegistry meterRegistry) {
        Counter rejectionCounter = Counter.builder("rate_limit_rejections_total")
                .description("Total number of rate-limited requests")
                .register(meterRegistry);

        this.rateLimiterInterceptor = new RateLimiterInterceptor(rateLimiter, rejectionCounter);
        this.rateLimiterInterceptor.addRule("default", defaultMax, Duration.ofSeconds(defaultWindowSeconds));
        this.rateLimiterInterceptor.addRule("POST:/api/reviews", reviewCreateMax, Duration.ofSeconds(reviewCreateWindowSeconds));
        this.rateLimiterInterceptor.addRule("GET:/api/stats", statsMax, Duration.ofSeconds(statsWindowSeconds));
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(rateLimiterInterceptor)
                .addPathPatterns("/api/**")
                .excludePathPatterns("/api/user", "/api/reviews/**", "/api/webhooks/**", "/actuator/**");
    }
}
