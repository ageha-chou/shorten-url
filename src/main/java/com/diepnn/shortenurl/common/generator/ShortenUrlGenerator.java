package com.diepnn.shortenurl.common.generator;

/**
 * Common contract for shorten URL generator
 */
public interface ShortenUrlGenerator<I,O> {
    /**
     * Generate shorten URL
     */
    O generate(I input);
}
