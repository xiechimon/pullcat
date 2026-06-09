package com.pullcat.service.analysis;

import java.time.Duration;

public interface RateLimiter {

    boolean isAllowed(String key, int maxRequests, Duration window);

    long getRemaining(String key, int maxRequests);
}
