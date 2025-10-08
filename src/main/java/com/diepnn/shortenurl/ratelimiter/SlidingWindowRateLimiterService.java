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

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class SlidingWindowRateLimiterService implements RateLimiterService {
    private static final String KEY_FORMAT = "rate-limiter::%s::%d";

    private final RateLimiterProperties props;
    private final RedisTemplate<String, Object> redisTemplate;
    private final ResourceLoader resourceLoader;
    private DefaultRedisScript<Long> rateLimiterScript;

    @PostConstruct
    public void init() {
        try {
            Resource scriptResource = resourceLoader.getResource("classpath:redis/scripts/sliding-window-rate-limit.lua");
            rateLimiterScript = new DefaultRedisScript<>();
            rateLimiterScript.setScriptSource(new ResourceScriptSource(scriptResource));
            rateLimiterScript.setResultType(Long.class);

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
            long currentTime = System.currentTimeMillis();
            long currentSubWindow = currentTime / props.getSubWindowSizeMs();
            long oldSubWindow = (currentTime - props.getWindowSizeMs()) / props.getSubWindowSizeMs();

            List<String> subWindowKeys = new ArrayList<>();
            for (long currWindow = oldSubWindow; currWindow <= currentSubWindow; ++currWindow) {
                subWindowKeys.add(String.format(KEY_FORMAT, key, currWindow));
            }

            long ttl = calculateTtl();
            Long result = redisTemplate.execute(rateLimiterScript,
                                                subWindowKeys,
                                                props.getLimit(),
                                                ttl
                                               );

            return result == 1L;
        } catch (Exception e) {
            log.error("Failed to check rate limiter for client: {}", key, e);
            return false;
        }
    }

    private long calculateTtl() {
        long proportionalBuffer = (long) (props.getWindowSizeMs() * 0.1);
        long buffer = Math.max(props.getSubWindowSizeMs(), Math.min(proportionalBuffer, 2 * props.getSubWindowSizeMs()));
        return props.getWindowSizeMs() + buffer;
    }
}
