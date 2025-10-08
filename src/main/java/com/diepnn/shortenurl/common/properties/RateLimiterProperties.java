package com.diepnn.shortenurl.common.properties;

import jakarta.annotation.PostConstruct;
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

    /**
     * Sub-window size in seconds (must be a divisor of windowSize)
     */
    private final long subWindowSizeMs;

    @PostConstruct
    public void validate() {
        if (windowSizeMs % subWindowSizeMs != 0) {
            throw new IllegalArgumentException("windowSizeMs must be divisible by subWindowSizeMs");
        }
    }
}
