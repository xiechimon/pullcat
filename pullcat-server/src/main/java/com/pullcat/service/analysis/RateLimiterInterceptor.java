package com.pullcat.service.analysis;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.servlet.HandlerInterceptor;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class RateLimiterInterceptor implements HandlerInterceptor {

    private final RateLimiter rateLimiter;
    private final Map<String, RateLimitRule> rules;
    private final Counter rejectionCounter;

    public RateLimiterInterceptor(RateLimiter rateLimiter, Counter rejectionCounter) {
        this.rateLimiter = rateLimiter;
        this.rejectionCounter = rejectionCounter;
        this.rules = new ConcurrentHashMap<>();
    }

    public void addRule(String pathPattern, int maxRequests, Duration window) {
        rules.put(pathPattern, new RateLimitRule(maxRequests, window));
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String path = request.getRequestURI();
        String method = request.getMethod();
        RateLimitRule rule = findRule(method + ":" + path);

        if (rule == null) {
            return true;
        }

        String key = buildKey(request, path);
        if (!rateLimiter.isAllowed(key, rule.maxRequests, rule.window)) {
            rejectionCounter.increment();
            response.setStatus(429);
            response.setHeader("Retry-After", String.valueOf(rule.window.getSeconds()));
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");
            response.getWriter().write("""
                    {"error":"Too Many Requests","message":"Rate limit exceeded. Try again later."}""");
            return false;
        }

        return true;
    }

    private RateLimitRule findRule(String key) {
        for (Map.Entry<String, RateLimitRule> entry : rules.entrySet()) {
            if (key.contains(entry.getKey())) {
                return entry.getValue();
            }
        }
        return rules.get("default");
    }

    private String buildKey(HttpServletRequest request, String path) {
        String user = request.getRemoteUser();
        if (user != null) {
            return "user:" + user + ":" + path;
        }
        return "ip:" + request.getRemoteAddr() + ":" + path;
    }

    private record RateLimitRule(int maxRequests, Duration window) {}
}
