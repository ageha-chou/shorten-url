package com.diepnn.shortenurl.ratelimiter;

import com.diepnn.shortenurl.common.properties.RateLimiterProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class SlidingWindowRateLimiterServiceTests {
    @Mock
    private RateLimiterProperties props;

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private DefaultRedisScript<Object> rateLimiterScript;

    private SlidingWindowRateLimiterService rateLimiterService;

    @BeforeEach
    void setUp() {
        lenient().when(props.getLimit()).thenReturn(10L);
        lenient().when(props.getWindowSizeMs()).thenReturn(1000L);
        lenient().when(props.getSubWindowSizeMs()).thenReturn(100L);

        rateLimiterService = new SlidingWindowRateLimiterService(
                props,
                redisTemplate,
                null
        );

        // Inject the mocked script directly, bypassing init()
        ReflectionTestUtils.setField(rateLimiterService, "rateLimiterScript", rateLimiterScript);
    }

    @Test
    void isAllowed_shouldReturnTrue_whenResultIsOne() {
        String clientKey = "user123";
        when(redisTemplate.execute(eq(rateLimiterScript), anyList(), any(), any()))
                .thenReturn(1L);

        boolean result = rateLimiterService.isAllowed(clientKey);

        assertTrue(result);
        verify(redisTemplate).execute(eq(rateLimiterScript), anyList(),  any(), any());
    }

    @Test
    void isAllowed_shouldReturnFalse_whenResultIsZero() {
        // Given
        String clientKey = "user123";
        when(redisTemplate.execute(eq(rateLimiterScript), anyList(), any(), any()))
                .thenReturn(0L);

        // When
        boolean result = rateLimiterService.isAllowed(clientKey);

        // Then
        assertFalse(result);
    }

    @Test
    void isAllowed_shouldReturnFalse_whenResultIsNull() {
        String clientKey = "user123";
        when(redisTemplate.execute(eq(rateLimiterScript), anyList(), any(), any()))
                .thenReturn(null);

        boolean result = rateLimiterService.isAllowed(clientKey);

        assertFalse(result);
    }

    @ParameterizedTest
    @NullAndEmptySource
    void isAllowed_shouldThrowException_whenKeyIsNullOrEmpty(String key) {
        assertThrows(IllegalArgumentException.class, () -> rateLimiterService.isAllowed(key));
    }

    @Test
    void isAllowed_shouldThrowException_whenKeyIsBlank() {
        assertThrows(IllegalArgumentException.class, () -> rateLimiterService.isAllowed("   "));
    }

    @Test
    void isAllowed_shouldReturnFalse_whenRedisThrowsException() {
        String clientKey = "user123";
        when(redisTemplate.execute(eq(rateLimiterScript), anyList(), any(), any()))
                .thenThrow(new RuntimeException("Redis connection failed"));

        boolean result = rateLimiterService.isAllowed(clientKey);

        assertFalse(result); // Fail-closed behavior
    }

    @Test
    void isAllowed_shouldGenerateCorrectSubWindowKeys() {
        String clientKey = "client123";
        ArgumentCaptor<List<String>> keysCaptor = ArgumentCaptor.forClass(List.class);
        when(redisTemplate.execute(eq(rateLimiterScript), keysCaptor.capture(), any(), any()))
                .thenReturn(1L);

        rateLimiterService.isAllowed(clientKey);

        List<String> capturedKeys = keysCaptor.getValue();
        assertNotNull(capturedKeys);
        assertFalse(capturedKeys.isEmpty());

        // Verify key format
        capturedKeys.forEach(key -> {
            assertTrue(key.startsWith("rate-limiter::client123::"),
                       "Key should start with 'rate-limiter::client123::' but was: " + key);
            assertTrue(key.matches("rate-limiter::client123::\\d+"),
                       "Key should match pattern 'rate-limiter::client123::<number>' but was: " + key);
        });
    }

    @Test
    void isAllowed_shouldCalculateCorrectNumberOfSubWindows() {
        String clientKey = "client123";
        ArgumentCaptor<List<String>> keysCaptor = ArgumentCaptor.forClass(List.class);
        when(redisTemplate.execute(eq(rateLimiterScript), keysCaptor.capture(), any(), any()))
                .thenReturn(1L);

        rateLimiterService.isAllowed(clientKey);

        List<String> capturedKeys = keysCaptor.getValue();

        // windowSize=1000ms, subWindowSize=100ms
        // oldSubWindow to currentSubWindow = 10 windows + currentKey added again = 11
        // The actual number depends on the implementation
        assertEquals(11, capturedKeys.size(), "Expected 11-12 sub-window keys but got: " + capturedKeys.size());
    }

    @Test
    void isAllowed_shouldPassCorrectArgumentsToScript() {
        String clientKey = "test-client";
        ArgumentCaptor<List<String>> keysCaptor = ArgumentCaptor.forClass(List.class);
        ArgumentCaptor<Long> arg1Captor = ArgumentCaptor.forClass(Long.class);
        ArgumentCaptor<Long> arg2Captor = ArgumentCaptor.forClass(Long.class);

        when(redisTemplate.execute(eq(rateLimiterScript), keysCaptor.capture(), arg1Captor.capture(), arg2Captor.capture()))
                .thenReturn(1L);

        rateLimiterService.isAllowed(clientKey);

        assertEquals(props.getLimit(), arg1Captor.getValue(), "First argument should be limit");

        // Verify TTL calculation (windowSize=1000, should add buffer)
        long ttl = arg2Captor.getValue();
        assertTrue(ttl > props.getWindowSizeMs(), "TTL should be greater than window size");
        assertTrue(ttl <= 2 * props.getWindowSizeMs(), "TTL should not be excessively large");
    }

    @Test
    void isAllowed_shouldHandleDifferentWindowSizes() {
        when(props.getWindowSizeMs()).thenReturn(60000L);
        when(props.getSubWindowSizeMs()).thenReturn(5000L);

        String clientKey = "short-window-client";
        ArgumentCaptor<List<String>> keysCaptor = ArgumentCaptor.forClass(List.class);
        when(redisTemplate.execute(eq(rateLimiterScript), keysCaptor.capture(), any(), any()))
                .thenReturn(1L);

        rateLimiterService.isAllowed(clientKey);

        // Then
        List<String> capturedKeys = keysCaptor.getValue();
        // 60000ms / 5000ms = 12 sub-windows + 1 extra (current) = 13
        assertEquals(13, capturedKeys.size(), "Expected 13-14 keys but got: " + capturedKeys.size());
    }

    @Test
    void isAllowed_shouldHandleMultipleCallsForSameClient() {
        // Given
        String clientKey = "repeat-client";
        when(redisTemplate.execute(eq(rateLimiterScript), anyList(), any(), any()))
                .thenReturn(1L, 1L, 0L); // Allow, Allow, Deny

        // When/Then
        assertTrue(rateLimiterService.isAllowed(clientKey));
        assertTrue(rateLimiterService.isAllowed(clientKey));
        assertFalse(rateLimiterService.isAllowed(clientKey));

        verify(redisTemplate, times(3)).execute(eq(rateLimiterScript), anyList(), any(), any());
    }

    @Test
    void isAllowed_shouldHandleDifferentClientKeys() {
        // Given
        when(redisTemplate.execute(eq(rateLimiterScript), anyList(), any(), any()))
                .thenReturn(1L);

        // When
        boolean result1 = rateLimiterService.isAllowed("client-A");
        boolean result2 = rateLimiterService.isAllowed("client-B");

        // Then
        assertTrue(result1);
        assertTrue(result2);
        verify(redisTemplate, times(2)).execute(eq(rateLimiterScript), anyList(), any(), any());
    }

    @Test
    void isAllowed_shouldNotCallRedis_whenKeyValidationFails() {
        assertThrows(IllegalArgumentException.class, () -> rateLimiterService.isAllowed(null));
        verify(redisTemplate, never()).execute(any(), anyList(), any(), any());
    }

    @Test
    void isAllowed_shouldUseCorrectKeyFormat() {
        String clientKey = "user:123:api";
        ArgumentCaptor<List<String>> keysCaptor = ArgumentCaptor.forClass(List.class);
        when(redisTemplate.execute(eq(rateLimiterScript), keysCaptor.capture(), any(), any()))
                .thenReturn(1L);

        rateLimiterService.isAllowed(clientKey);

        List<String> capturedKeys = keysCaptor.getValue();
        capturedKeys.forEach(key -> {
            assertTrue(key.startsWith("rate-limiter::user:123:api::"),
                       "Key should preserve client key exactly: " + key);
        });
    }

    @Test
    void isAllowed_shouldCalculateCorrectTtl_whenProportionalBufferIsInRange() {
        when(props.getWindowSizeMs()).thenReturn(60000L); // 60 seconds
        when(props.getSubWindowSizeMs()).thenReturn(5000L); // 5 seconds

        ArgumentCaptor<Long> ttlCaptor = ArgumentCaptor.forClass(Long.class);
        when(redisTemplate.execute(eq(rateLimiterScript), anyList(), any(), ttlCaptor.capture()))
                .thenReturn(1L);

        rateLimiterService.isAllowed("test");

        long ttl = ttlCaptor.getValue();
        // proportionalBuffer = 60000 * 0.1 = 6000
        // buffer = max(5000, min(6000, 10000)) = max(5000, 6000) = 6000
        // TTL = 60000 + 6000 = 66000
        assertEquals(66000L, ttl);
    }

    @Test
    void isAllowed_shouldCalculateCorrectTtl_whenProportionalBufferIsLarge() {
        // Given - proportional buffer exceeds 2*subWindowSize
        when(props.getWindowSizeMs()).thenReturn(600000L); // 600 seconds
        when(props.getSubWindowSizeMs()).thenReturn(10000L); // 10 seconds

        ArgumentCaptor<Long> ttlCaptor = ArgumentCaptor.forClass(Long.class);
        when(redisTemplate.execute(eq(rateLimiterScript), anyList(), any(), ttlCaptor.capture()))
                .thenReturn(1L);

        rateLimiterService.isAllowed("test");

        long ttl = ttlCaptor.getValue();
        // proportionalBuffer = 600000 * 0.1 = 60000
        // buffer = max(10000, min(60000, 20000)) = max(10000, 20000) = 20000
        // TTL = 600000 + 20000 = 620000
        assertEquals(620000L, ttl);
    }
}
