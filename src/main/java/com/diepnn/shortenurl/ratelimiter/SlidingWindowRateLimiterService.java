package com.diepnn.shortenurl.ratelimiter;

import com.diepnn.shortenurl.common.properties.RateLimiterProperties;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.scripting.support.ResourceScriptSource;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class SlidingWindowRateLimiterService implements RateLimiterService {
    private static final String KEY_FORMAT = "rate-limiter::%s";

    private final RateLimiterProperties props;
    private final RedisTemplate<String, Object> redisTemplate;
    private final ResourceLoader resourceLoader;
    private DefaultRedisScript<Long> rateLimiterScript;
    private long ttl;

    @PostConstruct
    public void init() {
        try {
            Resource scriptResource = resourceLoader.getResource("classpath:redis/scripts/sliding-window-rate-limit.lua");
            rateLimiterScript = new DefaultRedisScript<>();
            rateLimiterScript.setScriptSource(new ResourceScriptSource(scriptResource));
            rateLimiterScript.setResultType(Long.class);

            ttl = calculateTtl();

            log.info("Loaded Redis rate limiter script: {}", scriptResource.getFilename());
        } catch (Exception e) {
            log.error("Failed to load Redis rate limiter script", e);
            throw new IllegalStateException("Cannot initialize rate limiter without Lua script");
        }
    }

    @Override
    public boolean isAllowed(String key) {
        if (StringUtils.isBlank(key)) {
            throw new IllegalArgumentException("Key cannot be blank");
        }

        try {
            String redisKey = String.format(KEY_FORMAT, key);
            long currentTime = System.currentTimeMillis();

            // Generate unique request ID to prevent duplicate entries in sorted set
            String requestId = currentTime + ":" + UUID.randomUUID();

            Long result = redisTemplate.execute(rateLimiterScript,
                                                Collections.singletonList(redisKey),
                                                currentTime,
                                                props.getWindowSizeMs(),
                                                props.getLimit(),
                                                ttl,
                                                requestId
                                               );

            return result == 1L;
        } catch (Exception e) {
            log.error("Failed to check rate limiter for client: {}", key, e);
            // Fail closed: deny requests on errors
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
