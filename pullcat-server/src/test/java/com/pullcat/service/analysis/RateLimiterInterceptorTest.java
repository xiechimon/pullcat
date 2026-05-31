package com.pullcat.service.analysis;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RateLimiterInterceptorTest {

    private RateLimiter rateLimiter;
    private Counter rejectionCounter;
    private RateLimiterInterceptor interceptor;

    @BeforeEach
    void setUp() {
        MeterRegistry registry = mock(MeterRegistry.class);
        Counter counter = mock(Counter.class);
        when(registry.counter(anyString())).thenReturn(counter);
        rejectionCounter = counter;

        rateLimiter = mock(RateLimiter.class);
        interceptor = new RateLimiterInterceptor(rateLimiter, rejectionCounter);
    }

    @Test
    void allowsRequestWhenNoRuleMatches() throws Exception {
        HttpServletRequest request = request("GET", "/api/health");
        HttpServletResponse response = response();

        boolean result = interceptor.preHandle(request, response, null);

        assertThat(result).isTrue();
        verify(rateLimiter, never()).isAllowed(anyString(), anyInt(), any());
    }

    @Test
    void allowsRequestWithinRateLimit() throws Exception {
        interceptor.addRule("GET:/api/test", 10, Duration.ofMinutes(1));
        when(rateLimiter.isAllowed(anyString(), anyInt(), any())).thenReturn(true);
        HttpServletRequest request = request("GET", "/api/test");
        HttpServletResponse response = response();

        boolean result = interceptor.preHandle(request, response, null);

        assertThat(result).isTrue();
        verify(rejectionCounter, never()).increment();
    }

    @Test
    void rejectsRequestOverRateLimit() throws Exception {
        interceptor.addRule("GET:/api/test", 10, Duration.ofSeconds(30));
        when(rateLimiter.isAllowed(anyString(), anyInt(), any())).thenReturn(false);
        HttpServletRequest request = request("GET", "/api/test");
        HttpServletResponse response = responseWithWriter();

        boolean result = interceptor.preHandle(request, response, null);

        assertThat(result).isFalse();
        verify(response).setStatus(429);
        verify(response).setHeader("Retry-After", "30");
        verify(rejectionCounter, times(1)).increment();
    }

    @Test
    void fallsBackToDefaultRule() throws Exception {
        interceptor.addRule("default", 5, Duration.ofMinutes(1));
        when(rateLimiter.isAllowed(anyString(), anyInt(), any())).thenReturn(false);
        HttpServletRequest request = request("POST", "/api/unknown");
        HttpServletResponse response = responseWithWriter();

        boolean result = interceptor.preHandle(request, response, null);

        assertThat(result).isFalse();
        verify(response).setStatus(429);
        verify(rejectionCounter, times(1)).increment();
    }

    @Test
    void usesSpecificRuleOverDefault() throws Exception {
        interceptor.addRule("GET:/api/stats", 30, Duration.ofSeconds(60));
        interceptor.addRule("default", 5, Duration.ofMinutes(1));
        when(rateLimiter.isAllowed(anyString(), anyInt(), any())).thenReturn(false);
        HttpServletRequest request = request("GET", "/api/stats/overview");
        HttpServletResponse response = responseWithWriter();

        interceptor.preHandle(request, response, null);

        verify(response).setHeader("Retry-After", "60");
    }

    @Test
    void setsJsonContentTypeOnRejection() throws Exception {
        interceptor.addRule("GET:/api/test", 0, Duration.ofSeconds(10));
        when(rateLimiter.isAllowed(anyString(), anyInt(), any())).thenReturn(false);
        HttpServletRequest request = request("GET", "/api/test");
        HttpServletResponse response = responseWithWriter();

        interceptor.preHandle(request, response, null);

        verify(response).setContentType("application/json");
        verify(response).setCharacterEncoding("UTF-8");
    }

    private HttpServletRequest request(String method, String uri) {
        HttpServletRequest req = mock(HttpServletRequest.class);
        when(req.getMethod()).thenReturn(method);
        when(req.getRequestURI()).thenReturn(uri);
        when(req.getRemoteAddr()).thenReturn("127.0.0.1");
        when(req.getRemoteUser()).thenReturn(null);
        return req;
    }

    private HttpServletResponse response() {
        return mock(HttpServletResponse.class);
    }

    private HttpServletResponse responseWithWriter() {
        HttpServletResponse resp = mock(HttpServletResponse.class);
        try {
            when(resp.getWriter()).thenReturn(new PrintWriter(new StringWriter()));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return resp;
    }
}
