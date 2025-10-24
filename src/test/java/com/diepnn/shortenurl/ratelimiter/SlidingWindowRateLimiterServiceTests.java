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
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
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

    @Mock
    private ResourceLoader resourceLoader;

    private SlidingWindowRateLimiterService rateLimiterService;

    @BeforeEach
    void setUp() {
        lenient().when(props.getLimit()).thenReturn(10L);
        lenient().when(props.getWindowSizeMs()).thenReturn(1000L);

        rateLimiterService = new SlidingWindowRateLimiterService(
                props,
                redisTemplate,
                resourceLoader
        );

        // Inject the mocked script directly, bypassing init()
        ReflectionTestUtils.setField(rateLimiterService, "rateLimiterScript", rateLimiterScript);
    }

    @Test
    void isAllowed_shouldReturnTrue_whenResultIsOne() {
        String clientKey = "user123";
        when(redisTemplate.execute(eq(rateLimiterScript), anyList(), any(), any(), any(), any(), any()))
                .thenReturn(1L);

        boolean result = rateLimiterService.isAllowed(clientKey);

        assertTrue(result);
        verify(redisTemplate).execute(eq(rateLimiterScript), anyList(), any(), any(), any(), any(), any());
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
    void isAllowed_shouldPassCorrectArgumentsToScript() throws IOException {
        String clientKey = "test-client";
        ArgumentCaptor<List<String>> keysCaptor = ArgumentCaptor.forClass(List.class);
        ArgumentCaptor<Long> arg1Captor = ArgumentCaptor.forClass(Long.class);
        ArgumentCaptor<Long> arg2Captor = ArgumentCaptor.forClass(Long.class);
        ArgumentCaptor<Long> arg3Captor = ArgumentCaptor.forClass(Long.class);
        ArgumentCaptor<Long> arg4Captor = ArgumentCaptor.forClass(Long.class);
        ArgumentCaptor<String> arg5Captor = ArgumentCaptor.forClass(String.class);

        when(redisTemplate.execute(
                any(RedisScript.class),
                keysCaptor.capture(),
                arg1Captor.capture(),
                arg2Captor.capture(),
                arg3Captor.capture(),
                arg4Captor.capture(),
                arg5Captor.capture())
            ).thenReturn(1L);

        mockInit();
        rateLimiterService.isAllowed(clientKey);

        assertEquals(props.getWindowSizeMs(), arg2Captor.getValue(),
                     "Second argument should be window size");
        assertEquals(props.getLimit(), arg3Captor.getValue(),
                     "Third argument should be limit");

        long ttl = arg4Captor.getValue();
        assertTrue(ttl > props.getWindowSizeMs(),
                   String.format("TTL should be larger than window size but was %d", ttl));
        assertTrue(ttl <= 10000L, "TTL should not be excessively large");
    }

    @Test
    void isAllowed_shouldHandleDifferentWindowSizes() {
        when(props.getWindowSizeMs()).thenReturn(60000L);

        String clientKey = "short-window-client";
        ArgumentCaptor<List<String>> keysCaptor = ArgumentCaptor.forClass(List.class);
        when(redisTemplate.execute(eq(rateLimiterScript), keysCaptor.capture(), any(), any(), any(), any(), any()))
                .thenReturn(1L);

        rateLimiterService.isAllowed(clientKey);

        // Then
        List<String> capturedKeys = keysCaptor.getValue();
        assertEquals(1, capturedKeys.size(), "Expected 1 key but got: " + capturedKeys.size());
    }

    @Test
    void isAllowed_shouldHandleMultipleCallsForSameClient() {
        // Given
        String clientKey = "repeat-client";
        when(redisTemplate.execute(eq(rateLimiterScript), anyList(), any(), any(), any(), any(), any()))
                .thenReturn(1L, 1L, 0L); // Allow, Allow, Deny

        // When/Then
        assertTrue(rateLimiterService.isAllowed(clientKey));
        assertTrue(rateLimiterService.isAllowed(clientKey));
        assertFalse(rateLimiterService.isAllowed(clientKey));

        verify(redisTemplate, times(3)).execute(eq(rateLimiterScript), anyList(), any(), any(),
                                                                    any(), any(), any());
    }

    @Test
    void isAllowed_shouldHandleDifferentClientKeys() {
        // Given
        when(redisTemplate.execute(eq(rateLimiterScript), anyList(), any(), any(), any(), any(), any()))
                .thenReturn(1L);

        // When
        boolean result1 = rateLimiterService.isAllowed("client-A");
        boolean result2 = rateLimiterService.isAllowed("client-B");

        // Then
        assertTrue(result1);
        assertTrue(result2);
        verify(redisTemplate, times(2)).execute(eq(rateLimiterScript), anyList(), any(), any(), any(), any(), any());
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
        when(redisTemplate.execute(eq(rateLimiterScript), keysCaptor.capture(), any(), any(), any(), any(), any()))
                .thenReturn(1L);

        rateLimiterService.isAllowed(clientKey);

        List<String> capturedKeys = keysCaptor.getValue();
        capturedKeys.forEach(key -> {
            assertTrue(key.startsWith("rate-limiter::user:123:api"),
                       "Key should preserve client key exactly: " + key);
        });
    }

    @Test
    void isAllowed_shouldCalculateCorrectTtl_whenProportionalBufferIsInRange() {
        when(props.getWindowSizeMs()).thenReturn(60000L); // 60 seconds

        ArgumentCaptor<Long> ttlCaptor = ArgumentCaptor.forClass(Long.class);
        when(redisTemplate.execute(any(RedisScript.class), anyList(), any(), any(), any(), ttlCaptor.capture(), any()))
                .thenReturn(1L);
        mockInit();

        rateLimiterService.isAllowed("test");

        long ttl = ttlCaptor.getValue();

        long proportionalBuffer = (long) (props.getWindowSizeMs() * 0.1);
        long expectedTtl = props.getWindowSizeMs() + Math.max(1000L, Math.min(proportionalBuffer, 10000L));
        assertEquals(expectedTtl, ttl);
    }

    @Test
    void isAllowed_shouldCalculateCorrectTtl_whenProportionalBufferIsLarge() {
        when(props.getWindowSizeMs()).thenReturn(600000L); // 600 seconds

        ArgumentCaptor<Long> ttlCaptor = ArgumentCaptor.forClass(Long.class);
        when(redisTemplate.execute(any(RedisScript.class), anyList(), any(), any(), any(), ttlCaptor.capture(), any()))
                .thenReturn(1L);
        mockInit();

        rateLimiterService.isAllowed("test");

        long ttl = ttlCaptor.getValue();

        long proportionalBuffer = (long) (props.getWindowSizeMs() * 0.1);
        long expectedTtl = props.getWindowSizeMs() + Math.max(1000L, Math.min(proportionalBuffer, 10000L));
        assertEquals(expectedTtl, ttl);
    }

    private void mockInit() {
        // Mock the Resource
        Resource mockResource = mock(Resource.class);

        // Mock the Lua script content (optional, but good practice)
        String luaScript = """
            local key = KEYS[1]
            local now = tonumber(ARGV[1])
            local window = tonumber(ARGV[2])
            local limit = tonumber(ARGV[3])
            return 1
            """;

        // Mock ResourceLoader to return the mocked Resource
        when(resourceLoader.getResource("classpath:redis/scripts/sliding-window-rate-limit.lua"))
                .thenReturn(mockResource);

        rateLimiterService.init();
    }
}
