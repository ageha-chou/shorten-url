package com.diepnn.shortenurl.common.properties;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.rate-limiter")
@RequiredArgsConstructor
@Getter
public class RateLimiterProperties {
    /**
     * Maximum number of requests allowed within the window
     */
    private final long limit;

    /**
     * Total sliding window size in seconds
     */
    private final long windowSizeMs;
}
