package com.diepnn.shortenurl.common.properties;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for JWT signing and expiration.
 */
@ConfigurationProperties(prefix = "app.jwt")
@RequiredArgsConstructor
@Getter
public class JwtProperties {
    private final String secretKey;
    private final long ttl;
}
