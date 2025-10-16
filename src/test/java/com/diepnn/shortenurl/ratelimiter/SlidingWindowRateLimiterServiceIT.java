package com.diepnn.shortenurl.ratelimiter;

import com.diepnn.shortenurl.common.properties.RateLimiterProperties;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

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
@Testcontainers
public class SlidingWindowRateLimiterServiceIT {

    @Container
    static GenericContainer<?> redis = new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
            .withExposedPorts(6379);

    @DynamicPropertySource
    static void redisProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", redis::getFirstMappedPort);

        // Test properties - use longer windows to avoid timing issues
        registry.add("app.rate-limiter.limit", () -> 10);
        registry.add("app.rate-limiter.window-size-ms", () -> 1000);
        registry.add("app.rate-limiter.sub-window-size-ms", () -> 100);
    }

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

    @AfterEach
    void tearDown() {
        // Clean up test keys
        if (testClientKey != null) {
            Set<String> keys = redisTemplate.keys("rate-limiter::" + testClientKey + "::*");
            if (!keys.isEmpty()) {
                redisTemplate.delete(keys);
            }
        }
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
        Set<String> keys = redisTemplate.keys("rate-limiter::" + testClientKey + "::*");
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
        Set<String> client1Keys = redisTemplate.keys("rate-limiter::" + client1 + "::*");
        Set<String> client2Keys = redisTemplate.keys("rate-limiter::" + client2 + "::*");
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
        Set<String> keys = redisTemplate.keys("rate-limiter::" + testClientKey + "::*");
        assertNotNull(keys);
        assertFalse(keys.isEmpty());

        // Verify key format
        keys.forEach(key -> {
            assertTrue(key.matches("rate-limiter::" + testClientKey + "::\\d+"),
                       "Key should match pattern 'rate-limiter::<client>::<timestamp>' but was: " + key);
        });
    }

    @Test
    void isAllowed_shouldSetTtlOnRedisKeys() {
        // When
        rateLimiterService.isAllowed(testClientKey);

        // Then
        Set<String> keys = redisTemplate.keys("rate-limiter::" + testClientKey + "::*");
        assertNotNull(keys);
        assertFalse(keys.isEmpty());

        // Verify TTL is set on keys
        for (String key : keys) {
            Long ttl = redisTemplate.getExpire(key, TimeUnit.MILLISECONDS);
            assertNotNull(ttl, "TTL should not be null for key: " + key);
            assertTrue(ttl > 0, "TTL should be positive for key: " + key);

            // TTL should be window + buffer
            long windowSize = props.getWindowSizeMs();
            long subWindowSize = props.getSubWindowSizeMs();
            long minExpectedTtl = windowSize;
            long maxExpectedTtl = windowSize + (2 * subWindowSize);

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
        Set<String> keys = redisTemplate.keys("rate-limiter::" + clientKey + "::*");
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
        // When - make 5 requests
        for (int i = 0; i < 5; i++) {
            rateLimiterService.isAllowed(testClientKey);
        }

        // Then - verify counters in Redis
        Set<String> keys = redisTemplate.keys("rate-limiter::" + testClientKey + "::*");
        assertNotNull(keys);

        long totalCount = 0;
        for (String key : keys) {
            Object count = redisTemplate.opsForValue().get(key);
            if (count != null) {
                totalCount += Long.parseLong(count.toString());
            }
        }

        assertEquals(5L, totalCount, "Total count across all sub-windows should be 5");
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
    void isAllowed_shouldDistributeRequestsAcrossMultipleSubWindows() throws InterruptedException {
        // Given
        long subWindowSize = props.getSubWindowSizeMs();

        // When - make requests across different sub-windows
        rateLimiterService.isAllowed(testClientKey);

        // Wait for next sub-window
        Thread.sleep(subWindowSize);

        rateLimiterService.isAllowed(testClientKey);

        // Then - should have keys in different sub-windows
        Set<String> keys = redisTemplate.keys("rate-limiter::" + testClientKey + "::*");
        assertNotNull(keys);

        // Should have at least 2 different sub-window keys
        assertTrue(keys.size() >= 2,
                   "Should have keys in at least 2 different sub-windows, but got: " + keys.size());
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
        Set<String> keys = redisTemplate.keys("rate-limiter::" + longKey + "::*");
        assertNotNull(keys);
        assertFalse(keys.isEmpty());

        // Cleanup
        redisTemplate.delete(keys);
    }

    private long calculateExpectedTtl() {
        long proportionalBuffer = (long) (props.getWindowSizeMs() * 0.1);
        long buffer = Math.max(props.getSubWindowSizeMs(), Math.min(proportionalBuffer, 2 * props.getSubWindowSizeMs()));
        return props.getWindowSizeMs() + buffer;
    }
}