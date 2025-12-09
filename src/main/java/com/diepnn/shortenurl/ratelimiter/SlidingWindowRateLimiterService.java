package com.diepnn.shortenurl.ratelimiter;

import com.diepnn.shortenurl.common.properties.RateLimiterProperties;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class SlidingWindowRateLimiterService implements RateLimiterService {
    private static final String KEY_FORMAT = "rate-limiter::%s";

    private final RateLimiterProperties props;
    private final RedisTemplate<String, Object> redisTemplate;
    private long ttl;

    @PostConstruct
    public void init() {
        ttl = calculateTtl();
    }

    /**
     * Checks whether a request from the given client key should be allowed based on
     * a sliding window rate limit.
     *
     * <h3>Algorithm Overview</h3>
     * <p>This implementation uses a <strong>hybrid approach</strong> combining:</p>
     * <ul>
     *   <li><strong>Atomic counter</strong> ({@code INCR}) for fast, race-free limit checking</li>
     *   <li><strong>Sorted set</strong> ({@code ZSET}) for accurate time-based windowing and cleanup</li>
     * </ul>
     *
     * <h3>Why This Approach Works</h3>
     * <p>The key insight is that Redis's {@code INCR} command is <strong>truly atomic</strong> across
     * all concurrent clients. Unlike {@code ZCARD} (count sorted set size) followed by {@code ZADD},
     * which are two separate operations that can interleave, {@code INCR} guarantees that only one
     * thread can increment and read the result at a time.</p>
     *
     * <h3>Race Condition Prevention</h3>
     * <p><strong>Problem with naive approach:</strong></p>
     * <pre>
     * Thread A: ZCARD returns 1
     * Thread B: ZCARD returns 1  (both see same state!)
     * Thread A: ZADD (count becomes 2)
     * Thread B: ZADD (count becomes 3, exceeds limit of 2!)
     * </pre>
     *
     * <p><strong>Why INCR solves this:</strong></p>
     * <pre>
     * Thread A: INCR returns 1 (atomically increments and returns)
     * Thread B: INCR returns 2 (sees the incremented value from A)
     * Thread C: INCR returns 3 (immediately knows it exceeded limit of 2)
     * Thread C: DECR (rolls back its increment)
     * </pre>
     *
     * <h3>Step-by-Step Flow</h3>
     * <ol>
     *   <li><strong>Cleanup phase:</strong> Remove expired entries from sorted set and decrement
     *       the counter by the number of removed entries to keep counter synchronized.</li>
     *   <li><strong>Atomic check:</strong> Use {@code INCR} to atomically increment the counter.
     *       This is the critical section that prevents race conditions.</li>
     *   <li><strong>Limit enforcement:</strong> If counter exceeds limit, immediately {@code DECR}
     *       to rollback and deny the request.</li>
     *   <li><strong>Record keeping:</strong> If within limit, add the request to the sorted set
     *       for future cleanup with timestamp as score.</li>
     *   <li><strong>TTL management:</strong> Set expiration on both counter and sorted set to
     *       prevent memory leaks.</li>
     * </ol>
     *
     * <h3>Thread Safety Guarantees</h3>
     * <ul>
     *   <li>The counter never exceeds the limit across concurrent requests</li>
     *   <li>Each {@code INCR} operation is atomic and serialized by Redis</li>
     *   <li>Counter and sorted set may temporarily drift during cleanup, but this is safe because
     *       the counter is always the source of truth for admission decisions</li>
     * </ul>
     *
     * <h3>Trade-offs</h3>
     * <ul>
     *   <li><strong>Pro:</strong> Race-free, no retries needed, predictable performance</li>
     *   <li><strong>Pro:</strong> Simple implementation without distributed locks</li>
     *   <li><strong>Con:</strong> Uses two Redis keys (counter + sorted set) instead of one</li>
     *   <li><strong>Con:</strong> Counter may briefly be inaccurate during cleanup (but admission
     *       control is still correct)</li>
     * </ul>
     *
     * <h3>Error Handling</h3>
     * <p>Fails closed: returns {@code false} on any Redis exception to prevent overwhelming
     * downstream services during Redis outages.</p>
     *
     * @param key the client identifier (e.g., user ID, IP address, API key) to rate limit;
     *            must not be blank
     * @return {@code true} if the request is allowed (within rate limit), {@code false} if
     *         the request should be denied (rate limit exceeded or error occurred)
     * @throws IllegalArgumentException if {@code key} is blank
     */
    @Override
    public boolean isAllowed(String key) {
        if (StringUtils.isBlank(key)) {
            throw new IllegalArgumentException("Key cannot be blank");
        }

        try {
            String redisKey = String.format(KEY_FORMAT, key);
            String counterKey = redisKey + ":counter";
            long currentTime = System.currentTimeMillis();
            long windowStart = currentTime - props.getWindowSizeMs();
            String requestId = currentTime + ":" + UUID.randomUUID();

            // Remove old entries and get count of removed
            Long removed = redisTemplate.opsForZSet().removeRangeByScore(redisKey, 0, windowStart);

            // Decrement counter by removed count
            if (removed != null && removed > 0) {
                Long newCount = redisTemplate.opsForValue().decrement(counterKey, removed);
                // Ensure counter doesn't go negative
                if (newCount != null && newCount < 0) {
                    redisTemplate.opsForValue().set(counterKey, 0L);
                }
            }

            // Atomic increment
            Long count = redisTemplate.opsForValue().increment(counterKey);

            if (count == null || count > props.getLimit()) {
                // Exceeded limit, decrement back
                if (count != null) {
                    redisTemplate.opsForValue().decrement(counterKey);
                }
                return false;
            }

            // Add to sorted set
            redisTemplate.opsForZSet().add(redisKey, requestId, currentTime);

            // Set TTL
            redisTemplate.expire(redisKey, ttl, TimeUnit.MILLISECONDS);
            redisTemplate.expire(counterKey, ttl, TimeUnit.MILLISECONDS);

            return true;
        } catch (Exception e) {
            log.error("Failed to check rate limiter for client: {}", key, e);
            return false;
        }
    }

    private long calculateTtl() {
        // Add 10% buffer to window size to handle clock skew and cleanup delays
        long proportionalBuffer = (long) (props.getWindowSizeMs() * 0.1);
        // Ensure buffer is at least 1 second but no more than 10 seconds
        long buffer = Math.max(1000L, Math.min(proportionalBuffer, 10000L));
        return props.getWindowSizeMs() + buffer;
    }
}
