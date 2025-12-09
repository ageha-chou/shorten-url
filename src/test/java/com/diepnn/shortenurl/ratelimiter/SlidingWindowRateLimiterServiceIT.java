package com.diepnn.shortenurl.ratelimiter;

import com.diepnn.shortenurl.common.properties.RateLimiterProperties;
import com.diepnn.shortenurl.helper.BaseIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.RepeatedTest;
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
    private SlidingWindowRateLimiterService rateLimiterService;

    @Autowired
    private RateLimiterProperties props;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    private static final String TEST_KEY = "test-client";

    @Test
    @DisplayName("Should allow requests within limit")
    void testAllowWithinLimit() {
        long limit = props.getLimit();

        for (int i = 0; i < limit; i++) {
            assertTrue(rateLimiterService.isAllowed(TEST_KEY),
                       "Request " + (i + 1) + " should be allowed");
        }
    }

    @Test
    @DisplayName("Should deny requests exceeding limit")
    void testDenyExceedingLimit() {
        long limit = props.getLimit();

        // Fill up to limit
        for (int i = 0; i < limit; i++) {
            rateLimiterService.isAllowed(TEST_KEY);
        }

        // Next request should be denied
        assertFalse(rateLimiterService.isAllowed(TEST_KEY),
                    "Request exceeding limit should be denied");
    }

    @Test
    @DisplayName("Should allow requests after window expires")
    void testAllowAfterWindowExpires() throws InterruptedException {
        long limit = props.getLimit();
        long windowMs = props.getWindowSizeMs();

        // Fill up to limit
        for (int i = 0; i < limit; i++) {
            rateLimiterService.isAllowed(TEST_KEY);
        }

        // Should be denied
        assertFalse(rateLimiterService.isAllowed(TEST_KEY));

        // Wait for window to expire (add buffer for safety)
        Thread.sleep(windowMs + 100);

        // Should be allowed again
        assertTrue(rateLimiterService.isAllowed(TEST_KEY),
                   "Request should be allowed after window expires");
    }

    @Test
    @DisplayName("Should handle blank key")
    void testBlankKey() {
        assertThrows(IllegalArgumentException.class,
                     () -> rateLimiterService.isAllowed(""),
                     "Should throw exception for blank key");

        assertThrows(IllegalArgumentException.class,
                     () -> rateLimiterService.isAllowed(null),
                     "Should throw exception for null key");

        assertThrows(IllegalArgumentException.class,
                     () -> rateLimiterService.isAllowed("   "),
                     "Should throw exception for whitespace key");
    }

    @Test
    @DisplayName("Should isolate different keys")
    void testKeyIsolation() {
        long limit = props.getLimit();

        String key1 = TEST_KEY + "-1";
        String key2 = TEST_KEY + "-2";

        // Fill limit for key1
        for (int i = 0; i < limit; i++) {
            rateLimiterService.isAllowed(key1);
        }

        // key1 should be denied
        assertFalse(rateLimiterService.isAllowed(key1));

        // key2 should still be allowed (independent limit)
        assertTrue(rateLimiterService.isAllowed(key2),
                   "Different keys should have independent rate limits");
    }

    @Test
    @DisplayName("Should handle concurrent requests without race conditions")
    void testConcurrentRequests() throws InterruptedException {
        int threadCount = 20;
        int requestsPerThread = 5;
        long limit = props.getLimit();

        String testKey = TEST_KEY + "-concurrent-" + System.nanoTime();

        AtomicInteger allowed = new AtomicInteger(0);
        AtomicInteger denied = new AtomicInteger(0);

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await(); // All threads start together
                    for (int j = 0; j < requestsPerThread; j++) {
                        if (rateLimiterService.isAllowed(testKey)) {
                            allowed.incrementAndGet();
                        } else {
                            denied.incrementAndGet();
                        }
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown(); // Start all threads
        assertTrue(doneLatch.await(10, TimeUnit.SECONDS),
                   "Test timeout - threads didn't complete");
        executor.shutdown();

        int totalRequests = threadCount * requestsPerThread;
        assertEquals(totalRequests, allowed.get() + denied.get(),
                     "Total requests should match");

        assertTrue(allowed.get() <= limit,
                   String.format("RACE CONDITION: Allowed %d requests but limit is %d",
                                 allowed.get(), limit));

        assertEquals(limit, allowed.get(),
                     String.format("Should allow exactly %d requests", limit));
    }

    @RepeatedTest(50)
    @DisplayName("Should never exceed limit under high contention (repeated)")
    void testHighContentionRepeated() throws InterruptedException {
        int threadCount = 25;
        int requestsPerThread = 2;
        long limit = props.getLimit();

        String testKey = TEST_KEY + "-contention-" + System.nanoTime();

        AtomicInteger allowed = new AtomicInteger(0);

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await();
                    for (int j = 0; j < requestsPerThread; j++) {
                        if (rateLimiterService.isAllowed(testKey)) {
                            allowed.incrementAndGet();
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
        assertTrue(doneLatch.await(10, TimeUnit.SECONDS));
        executor.shutdown();

        assertTrue(allowed.get() <= limit,
                   String.format("Race condition detected: allowed=%d, limit=%d",
                                 allowed.get(), limit));
    }

    @Test
    @DisplayName("Should handle sliding window correctly")
    void testSlidingWindow() throws InterruptedException {
        long limit = props.getLimit();
        long windowMs = props.getWindowSizeMs();
        long halfWindow = windowMs / 2;

        // Make requests at T=0
        for (int i = 0; i < limit; i++) {
            rateLimiterService.isAllowed(TEST_KEY);
        }

        // Should be denied immediately
        assertFalse(rateLimiterService.isAllowed(TEST_KEY));

        // Wait for half the window
        Thread.sleep(halfWindow);

        // Still should be denied (original requests still in window)
        assertFalse(rateLimiterService.isAllowed(TEST_KEY));

        // Wait for the rest of the window
        Thread.sleep(halfWindow + 100);

        // Now should be allowed (original requests expired)
        assertTrue(rateLimiterService.isAllowed(TEST_KEY));
    }

    @Test
    @DisplayName("Should maintain counter accuracy after cleanup")
    void testCounterAccuracyAfterCleanup() throws InterruptedException {
        long limit = props.getLimit();
        long windowMs = props.getWindowSizeMs();

        // Make half the limit requests
        long halfLimit = limit / 2;
        for (int i = 0; i < halfLimit; i++) {
            rateLimiterService.isAllowed(TEST_KEY);
        }

        // Wait for window to expire
        Thread.sleep(windowMs + 100);

        // Make full limit of new requests - should all be allowed
        int allowed = 0;
        for (int i = 0; i < limit; i++) {
            if (rateLimiterService.isAllowed(TEST_KEY)) {
                allowed++;
            }
        }

        assertEquals(limit, allowed,
                     "After cleanup, should allow full limit of new requests");
    }

    @Test
    @DisplayName("Should handle burst traffic correctly")
    void testBurstTraffic() throws InterruptedException {
        long limit = props.getLimit();

        ExecutorService executor = Executors.newFixedThreadPool(50);
        CountDownLatch latch = new CountDownLatch((int) limit * 2);
        AtomicInteger allowed = new AtomicInteger(0);

        // Create burst of requests (2x the limit)
        for (int i = 0; i < limit * 2; i++) {
            executor.submit(() -> {
                try {
                    if (rateLimiterService.isAllowed(TEST_KEY)) {
                        allowed.incrementAndGet();
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        assertTrue(latch.await(10, TimeUnit.SECONDS));
        executor.shutdown();

        assertEquals(limit, allowed.get(),
                     "Burst traffic should only allow up to limit");
    }

    @Test
    @DisplayName("Should handle mixed sequential and concurrent patterns")
    void testMixedPatterns() throws InterruptedException {
        long limit = props.getLimit();
        String testKey = TEST_KEY + "-mixed-" + System.nanoTime();

        // Sequential requests (half the limit)
        long sequential = limit / 2;
        for (int i = 0; i < sequential; i++) {
            assertTrue(rateLimiterService.isAllowed(testKey));
        }

        // Concurrent requests (try to exceed remaining limit)
        long remaining = limit - sequential;
        int threadCount = 10;
        AtomicInteger allowed = new AtomicInteger(0);

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await();
                    if (rateLimiterService.isAllowed(testKey)) {
                        allowed.incrementAndGet();
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        assertTrue(doneLatch.await(10, TimeUnit.SECONDS));
        executor.shutdown();

        long totalAllowed = sequential + allowed.get();
        assertTrue(totalAllowed <= limit,
                   String.format("Total allowed (%d) should not exceed limit (%d)",
                                 totalAllowed, limit));
    }

    @Test
    @DisplayName("Should properly expire keys with TTL")
    void testTTLExpiration() throws InterruptedException {
        String testKey = TEST_KEY + "-ttl-" + System.nanoTime();

        // Make one request
        rateLimiterService.isAllowed(testKey);

        // Verify keys exist
        String redisKey = String.format("rate-limiter::%s", testKey);
        String counterKey = redisKey + ":counter";

        assertTrue(redisTemplate.hasKey(redisKey), "Redis key should exist");
        assertTrue(redisTemplate.hasKey(counterKey), "Counter key should exist");

        // Check TTL is set (should be greater than 0)
        Long ttl = redisTemplate.getExpire(redisKey, TimeUnit.MILLISECONDS);
        assertNotNull(ttl);
        assertTrue(ttl > 0, "TTL should be set on redis key");

        Long counterTtl = redisTemplate.getExpire(counterKey, TimeUnit.MILLISECONDS);
        assertNotNull(counterTtl);
        assertTrue(counterTtl > 0, "TTL should be set on counter key");
    }
}