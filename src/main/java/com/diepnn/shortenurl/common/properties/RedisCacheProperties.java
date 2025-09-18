package com.diepnn.shortenurl.common.properties;

import lombok.Getter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Map;

/**
 * Configuration properties for cache TTLs {@systemProperty app.cache-ttl.*} and types {@systemProperty app.cache-type.*}.
 */
@ConfigurationProperties(prefix = "app")
@Getter
public class RedisCacheProperties {
    private final Map<String, Long> cacheTtl;
    private final Map<String, Class<?>> cacheType;

    public RedisCacheProperties(Map<String, Long> cacheTtl, Map<String, Class<?>> cacheType) {
        this.cacheTtl = cacheTtl;
        this.cacheType = cacheType;
    }
}
