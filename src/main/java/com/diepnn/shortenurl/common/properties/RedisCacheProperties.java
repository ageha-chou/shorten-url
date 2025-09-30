package com.diepnn.shortenurl.common.properties;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Map;

/**
 * Configuration properties for cache TTLs {@systemProperty app.cache-ttl.*} and types {@systemProperty app.cache-type.*}.
 */
@ConfigurationProperties(prefix = "app")
@Getter
@RequiredArgsConstructor
public class RedisCacheProperties {
    private final Map<String, Long> cacheTtl;
    private final Map<String, String> cacheType;
}
