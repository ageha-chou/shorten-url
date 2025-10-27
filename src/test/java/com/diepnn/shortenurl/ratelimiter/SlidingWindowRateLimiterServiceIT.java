package com.diepnn.shortenurl.ratelimiter;

import com.diepnn.shortenurl.common.properties.RateLimiterProperties;
import com.diepnn.shortenurl.helper.BaseIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.context.TestPropertySource;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@TestPropertySource(properties = {
        "app.rate-limiter.limit=10",
        "app.rate-limiter.window-size-ms=1000"
})
public class SlidingWindowRateLimiterServiceIT extends BaseIntegrationTest {
    @Autowired
    private RateLimiterService rateLimiterService;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private RateLimiterProperties props;

    private String testClientKey;

    @BeforeEach
    void setUp() {
        // Generate unique client key for each test to avoid interference
        testClientKey = "test-" + UUID.randomUUID();
    }

    @Test
    void isAllowed_shouldAllowRequests_whenBelowLimit() {
        // Given
        long limit = props.getLimit();

        // When - make requests below limit
        for (long i = 0; i < limit; i++) {
            boolean result = rateLimiterService.isAllowed(testClientKey);
            assertTrue(result, "Request " + (i + 1) + " should be allowed");
        }

        // Then - verify Redis keys were created
        Set<String> keys = redisTemplate.keys("rate-limiter::" + testClientKey);
        assertNotNull(keys);
        assertFalse(keys.isEmpty());
    }

    @Test
    void isAllowed_shouldDenyRequest_whenLimitExceeded() {
        // Given
        long limit = props.getLimit();

        // When - exhaust the limit
        for (long i = 0; i < limit; i++) {
            assertTrue(rateLimiterService.isAllowed(testClientKey), "Request " + (i + 1) + " should be allowed");
        }

        // Then - next request should be denied
        assertFalse(rateLimiterService.isAllowed(testClientKey), "Request should be denied after exceeding limit");
    }

    @Test
    void isAllowed_shouldEnforceLimit_acrossMultipleRequests() {
        // Given
        long limit = props.getLimit();
        long totalRequests = limit + 5;
        long allowedCount = 0;

        // When
        for (long i = 0; i < totalRequests; i++) {
            if (rateLimiterService.isAllowed(testClientKey)) {
                allowedCount++;
            }
        }

        // Then
        assertEquals(limit, allowedCount, "Should allow exactly " + limit + " requests");
    }

    @Test
    void isAllowed_shouldIsolateDifferentClients() {
        // Given
        String client1 = "client-1-" + UUID.randomUUID();
        String client2 = "client-2-" + UUID.randomUUID();
        long limit = props.getLimit();

        // When - exhaust client1's limit
        for (long i = 0; i < limit; i++) {
            assertTrue(rateLimiterService.isAllowed(client1));
        }
        assertFalse(rateLimiterService.isAllowed(client1), "Client 1 should be rate limited");

        // Then - client2 should still be allowed
        assertTrue(rateLimiterService.isAllowed(client2), "Client 2 should not be affected by client 1's rate limit");

        // Verify separate Redis keys
        Set<String> client1Keys = redisTemplate.keys("rate-limiter::" + client1);
        Set<String> client2Keys = redisTemplate.keys("rate-limiter::" + client2);
        assertNotNull(client1Keys);
        assertNotNull(client2Keys);
        assertFalse(client1Keys.isEmpty());
        assertFalse(client2Keys.isEmpty());

        // Cleanup
        redisTemplate.delete(client1Keys);
        redisTemplate.delete(client2Keys);
    }

    @Test
    void isAllowed_shouldHandleConcurrentRequests_withoutExceedingLimit() throws InterruptedException {
        // Given
        long limit = props.getLimit();
        int threadCount = 30; // More threads than limit
        int requestsPerThread = 2;
        int totalRequests = threadCount * requestsPerThread; // 60 total requests

        AtomicInteger allowedCount = new AtomicInteger(0);
        AtomicInteger deniedCount = new AtomicInteger(0);

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);

        // When
        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await();
                    for (int j = 0; j < requestsPerThread; j++) {
                        if (rateLimiterService.isAllowed(testClientKey)) {
                            allowedCount.incrementAndGet();
                        } else {
                            deniedCount.incrementAndGet();
                        }
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        boolean completed = doneLatch.await(10, TimeUnit.SECONDS);
        executor.shutdown();

        // Then
        assertTrue(completed, "All threads should complete within timeout");
        assertEquals(totalRequests, allowedCount.get() + deniedCount.get(),
                     "Total requests should equal allowed + denied");

        // The key assertion: some requests MUST be denied
        assertTrue(allowedCount.get() <= limit,
                   "Allowed count (" + allowedCount.get() + ") should not exceed limit (" + limit + ")");
        assertTrue(deniedCount.get() > 0,
                   "Some requests should be denied when exceeding limit. Denied: " + deniedCount.get());
    }

    @Test
    void isAllowed_shouldCreateKeysWithCorrectFormat() {
        // When
        rateLimiterService.isAllowed(testClientKey);

        // Then
        Set<String> keys = redisTemplate.keys("rate-limiter::" + testClientKey);
        assertNotNull(keys);
        assertFalse(keys.isEmpty());

        // Verify key format
        keys.forEach(key -> {
            assertTrue(key.matches("rate-limiter::" + testClientKey),
                       "Key should match pattern 'rate-limiter::<client>' but was: " + key);
        });
    }

    @Test
    void isAllowed_shouldSetTtlOnRedisKeys() {
        // When
        rateLimiterService.isAllowed(testClientKey);

        // Then
        Set<String> keys = redisTemplate.keys("rate-limiter::" + testClientKey);
        assertNotNull(keys);
        assertFalse(keys.isEmpty());

        // Verify TTL is set on keys
        for (String key : keys) {
            Long ttl = redisTemplate.getExpire(key, TimeUnit.MILLISECONDS);
            assertNotNull(ttl, "TTL should not be null for key: " + key);
            assertTrue(ttl > 0, "TTL should be positive for key: " + key);

            // TTL should be window + buffer
            long minExpectedTtl = props.getWindowSizeMs();
            long maxExpectedTtl = 10000L;

            assertTrue(ttl >= minExpectedTtl && ttl <= maxExpectedTtl,
                       "TTL (" + ttl + "ms) should be between " + minExpectedTtl + "ms and " + maxExpectedTtl + "ms");
        }
    }

    @Test
    void isAllowed_shouldAllowRequestsAfterWindowExpires() throws InterruptedException {
        // Given
        long limit = props.getLimit();

        // When - fill up the limit
        for (long i = 0; i < limit; i++) {
            assertTrue(rateLimiterService.isAllowed(testClientKey));
        }

        // Should be denied immediately
        assertFalse(rateLimiterService.isAllowed(testClientKey));

        // Wait for window to expire
        Thread.sleep(calculateExpectedTtl());

        // Then - should allow requests again after window expires
        assertTrue(rateLimiterService.isAllowed(testClientKey), "Should allow requests after window expires");
    }

    @Test
    void isAllowed_shouldHandleSpecialCharactersInClientKey() {
        // Given
        String clientKey = "user:123:api-key-" + UUID.randomUUID();

        // When
        boolean result = rateLimiterService.isAllowed(clientKey);

        // Then
        assertTrue(result);

        // Verify key was created correctly
        Set<String> keys = redisTemplate.keys("rate-limiter::" + clientKey);
        assertNotNull(keys);
        assertFalse(keys.isEmpty());

        // Cleanup
        redisTemplate.delete(keys);
    }

    @Test
    void isAllowed_shouldThrowException_whenKeyIsBlank() {
        // When/Then
        assertThrows(IllegalArgumentException.class, () -> rateLimiterService.isAllowed(""));

        assertThrows(IllegalArgumentException.class, () -> rateLimiterService.isAllowed("   "));

        assertThrows(IllegalArgumentException.class, () -> rateLimiterService.isAllowed(null));
    }

    @Test
    void isAllowed_shouldIncrementCountersCorrectly() {
        int requestCount = 5;
        for (int i = 0; i < requestCount; i++) {
            rateLimiterService.isAllowed(testClientKey);
        }

        String keys = "rate-limiter::" + testClientKey;
        assertNotNull(keys);

        Long totalCount = redisTemplate.opsForZSet().count(keys, 0, System.currentTimeMillis());
        assertEquals(requestCount, totalCount, "Total allowed requests should be + " + requestCount + ", but was: " + totalCount);
    }

    @Test
    void isAllowed_shouldHandleRapidSuccessiveRequests() {
        // Given
        long limit = props.getLimit();

        // When - make rapid requests
        int allowedCount = 0;
        for (long i = 0; i < limit * 2; i++) {
            if (rateLimiterService.isAllowed(testClientKey)) {
                allowedCount++;
            }
        }

        // Then
        assertEquals(limit, allowedCount, "Should allow exactly " + limit + " requests even when made rapidly");
    }

    @Test
    void init_shouldLoadLuaScriptSuccessfully() {
        // Given/When - service is already initialized via @PostConstruct

        // Then - service should work correctly
        boolean result = rateLimiterService.isAllowed(testClientKey);

        assertTrue(result, "Service should work after successful initialization");
    }

    @Test
    void isAllowed_shouldHandleVeryLongClientKeys() {
        // Given
        String longKey = "client-" + "x".repeat(200) + "-" + UUID.randomUUID();

        // When
        boolean result = rateLimiterService.isAllowed(longKey);

        // Then
        assertTrue(result);

        // Verify key was stored
        Set<String> keys = redisTemplate.keys("rate-limiter::" + longKey);
        assertNotNull(keys);
        assertFalse(keys.isEmpty());

        // Cleanup
        redisTemplate.delete(keys);
    }

    private long calculateExpectedTtl() {
        long proportionalBuffer = (long) (props.getWindowSizeMs() * 0.1);
        long buffer = Math.max(1000L, Math.min(proportionalBuffer, 10000L));
        return props.getWindowSizeMs() + buffer;
    }
}