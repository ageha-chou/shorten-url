package com.diepnn.shortenurl.common.generator;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.shorten.strategy", havingValue = "base62", matchIfMissing = true)
public class ShortenUrlBase62Generator implements ShortenUrlGenerator<Long, String> {
    @Override
    public String generate(Long original) {
        return "";
    }
}
