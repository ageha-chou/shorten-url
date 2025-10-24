package com.diepnn.shortenurl.ratelimiter;

import com.diepnn.shortenurl.common.properties.RateLimiterProperties;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@Testcontainers
@Tag("stress")
public class SlidingWindowRateLimiterStressTest {
    @Autowired
    private SlidingWindowRateLimiterService rateLimiterService;

    @Autowired
    private RateLimiterProperties props;

    private static final String TEST_KEY = "stress-test-client";

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
    }

    // Run the test 100 times to catch race conditions
    @RepeatedTest(100)
    void concurrentRequests_shouldNeverExceedLimit_repeatedTest() throws InterruptedException {
        String testKey = TEST_KEY + "-" + System.nanoTime();
        assertConcurrentSafety(testKey, 30, 2);
    }

    // Single comprehensive test with multiple iterations
    @Test
    void concurrentRequests_shouldNeverExceedLimit_multipleIterations() throws InterruptedException {
        int iterations = 200;
        AtomicInteger failures = new AtomicInteger(0);

        for (int i = 0; i < iterations; i++) {
            String testKey = TEST_KEY + "-iter-" + i + "-" + System.nanoTime();
            try {
                assertConcurrentSafety(testKey, 25, 2);
            } catch (AssertionError e) {
                failures.incrementAndGet();
                System.err.println("Iteration " + i + " failed: " + e.getMessage());
            }
        }

        assertEquals(0, failures.get(),
                     "Race conditions detected in " + failures.get() + "/" + iterations + " iterations");
    }

    // Extreme stress test with even more contention
    @Test
    void extremeContention_shouldNeverExceedLimit() throws InterruptedException {
        String testKey = TEST_KEY + "-extreme-" + System.nanoTime();
        long limit = props.getLimit();

        // 100 threads all trying to make requests simultaneously
        int threadCount = 100;
        int requestsPerThread = 1;

        AtomicInteger allowed = new AtomicInteger(0);
        AtomicInteger denied = new AtomicInteger(0);

        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await();
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

        startLatch.countDown();
        assertTrue(doneLatch.await(30, TimeUnit.SECONDS));
        executor.shutdown();

        assertTrue(allowed.get() <= limit,
                   String.format("RACE CONDITION: Allowed %d requests but limit is %d",
                                 allowed.get(), limit));
    }

    // Test with rapid sequential bursts
    @Test
    void rapidBursts_shouldRespectLimit() throws InterruptedException {
        String testKey = TEST_KEY + "-burst-" + System.nanoTime();
        long limit = props.getLimit();

        // Send 3 waves of concurrent requests
        for (int wave = 0; wave < 3; wave++) {
            AtomicInteger allowed = new AtomicInteger(0);

            ExecutorService executor = Executors.newFixedThreadPool(20);
            CountDownLatch latch = new CountDownLatch(20);

            for (int i = 0; i < 20; i++) {
                executor.submit(() -> {
                    try {
                        if (rateLimiterService.isAllowed(testKey)) {
                            allowed.incrementAndGet();
                        }
                    } finally {
                        latch.countDown();
                    }
                });
            }

            latch.await(5, TimeUnit.SECONDS);
            executor.shutdown();

            // First wave should allow up to limit, subsequent waves should be denied
            if (wave == 0) {
                assertTrue(allowed.get() <= limit,
                           "First wave exceeded limit: " + allowed.get());
            }
        }
    }

    // Parallel test across multiple keys
    @Test
    void multipleKeys_shouldIsolateRateLimits() throws InterruptedException {
        int keyCount = 10;
        long limit = props.getLimit();

        CountDownLatch doneLatch = new CountDownLatch(keyCount);
        ExecutorService executor = Executors.newFixedThreadPool(keyCount);
        ConcurrentHashMap<String, Integer> allowedPerKey = new ConcurrentHashMap<>();

        for (int k = 0; k < keyCount; k++) {
            String testKey = TEST_KEY + "-multikey-" + k + "-" + System.nanoTime();
            executor.submit(() -> {
                try {
                    AtomicInteger allowed = new AtomicInteger(0);

                    // 20 concurrent requests per key
                    CountDownLatch keyLatch = new CountDownLatch(20);
                    ExecutorService keyExecutor = Executors.newFixedThreadPool(20);

                    for (int i = 0; i < 20; i++) {
                        keyExecutor.submit(() -> {
                            try {
                                if (rateLimiterService.isAllowed(testKey)) {
                                    allowed.incrementAndGet();
                                }
                            } finally {
                                keyLatch.countDown();
                            }
                        });
                    }

                    keyLatch.await(5, TimeUnit.SECONDS);
                    keyExecutor.shutdown();

                    allowedPerKey.put(testKey, allowed.get());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        assertTrue(doneLatch.await(30, TimeUnit.SECONDS));
        executor.shutdown();

        // Each key should independently respect the limit
        allowedPerKey.forEach((key, count) -> {
            assertTrue(count <= limit,
                       String.format("Key %s exceeded limit: %d > %d", key, count, limit));
        });
    }

    // Helper method for basic concurrent safety testing
    private void assertConcurrentSafety(String testKey, int threadCount, int requestsPerThread)
    throws InterruptedException {
        long limit = props.getLimit();
        int totalRequests = threadCount * requestsPerThread;

        AtomicInteger allowed = new AtomicInteger(0);
        AtomicInteger denied = new AtomicInteger(0);

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

        startLatch.countDown();
        assertTrue(doneLatch.await(10, TimeUnit.SECONDS),
                   "Test timeout - threads didn't complete");
        executor.shutdown();

        assertEquals(totalRequests, allowed.get() + denied.get(),
                     "Request count mismatch");

        assertTrue(allowed.get() <= limit,
                   String.format("RACE CONDITION DETECTED: Allowed %d requests but limit is %d",
                                 allowed.get(), limit));

        assertTrue(denied.get() > 0,
                   "Expected some requests to be denied when exceeding limit");
    }
}
