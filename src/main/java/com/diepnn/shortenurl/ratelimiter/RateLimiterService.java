package com.diepnn.shortenurl.ratelimiter;

/**
 * Rate limiter service interface.
 */
public interface RateLimiterService {
    /**
     * Check if the given key is allowed to access the service.
     *
     * @param key the key to check
     * @return true if the key is allowed, false otherwise
     */
    boolean isAllowed(String key);
}
