package com.diepnn.shortenurl.security;

import com.diepnn.shortenurl.common.properties.JwtProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * Service for managing JWT cache entries.
 */
@Service
@RequiredArgsConstructor
public class JwtCacheService {
    private final JwtProperties jwtProperties;
    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;

    private static final String CACHE_FORMAT = "authenticated-user::%s";

    /**
     * Adds a new entry to the cache.
     *
     * @param username username of the authenticated user
     * @param userDetails user details object
     */
    public void add(String username, CustomUserDetails userDetails) {
        if (userDetails == null) return;
        redisTemplate.opsForValue().set(String.format(CACHE_FORMAT, username),
                                        userDetails,
                                        Duration.ofMillis(jwtProperties.getTtl()));
    }

    /**
     * Checks if the cache contains an entry for the given username.
     *
     * @param username username of the authenticated user
     * @return cached user, or null if not found.
     */
    public CustomUserDetails getCacheUser(String username) {
        return objectMapper.convertValue(redisTemplate.opsForValue().get(String.format(CACHE_FORMAT, username)), CustomUserDetails.class);
    }

    /**
     * Removes the entry for the given username from the cache.
     *
     * @param username username of the authenticated user
     */
    public void remove(String username) {
        redisTemplate.delete(String.format(CACHE_FORMAT, username));
    }
}
