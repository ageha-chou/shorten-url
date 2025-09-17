package com.diepnn.shortenurl.mapper.translator;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class ShortUrlMappingsTests {
    private ShortUrlMappings shortUrlMappings = new ShortUrlMappings("http://localhost:8080");

    @ParameterizedTest
    @NullAndEmptySource
    void toShortUrl_whenShortCodeIsNullOrBlank_returnNull(String input) {
        String shortUrl = shortUrlMappings.toShortUrl(input);
        assertNull(shortUrl, "ShortUrl should be null");
    }

    @Test
    void toShortUrl_whenShortCodeIsValid_returnShortUrl() {
        String shortUrl = shortUrlMappings.toShortUrl("abc123");
        assertEquals("http://localhost:8080/abc123", shortUrl, "Wrong shortUrl");
    }

    @ParameterizedTest
    @NullAndEmptySource
    void fromShortUrl_whenShortCodeIsNull_returnNull(String input) {
        String shortUrl = shortUrlMappings.fromShortUrl(input);
        assertNull(shortUrl, "ShortUrl should be null");
    }

    @Test
    void fromShortUrl_whenShortCodeIsValid_returnShortCode() {
        String shortUrl = shortUrlMappings.fromShortUrl("http://localhost:8080/abc123");
        assertEquals("abc123", shortUrl, "Wrong shortUrl");
    }

    @ParameterizedTest
    @ValueSource(strings = {"http://localhost:8080/abc123", "https://localhost:8080/abc123"})
    void normalizeUrl_whenUrlStartsWithHttp_returnUrl(String inputUrl) {
        String url = shortUrlMappings.normalizeUrl(inputUrl);
        assertEquals(url, inputUrl, "Wrong url");
    }

    @Test
    void normalizeUrl_whenUrlDoesNotStartWithHttp_returnUrlWithHttp() {
        String url = shortUrlMappings.normalizeUrl("localhost:8080/abc123");
        assertEquals("http://localhost:8080/abc123", url, "Wrong url");
    }

    @Test
    void validateOriginalUrl_whenOriginalUrlIsNullOrBlank_throwIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> shortUrlMappings.validateOriginalUrl(null));
        assertThrows(IllegalArgumentException.class, () -> shortUrlMappings.validateOriginalUrl(" "));
    }

    @ParameterizedTest
    @ValueSource(strings = {"http://localhost:8080", "https://localhost:8080", "localhost:8080"})
    void validateOriginalUrl_whenOriginalUrlIsSameHostWithBase_throwIllegalArgumentException(String inputUrl) {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> shortUrlMappings.validateOriginalUrl(inputUrl));
        assertEquals("Prohibited domain: localhost", ex.getMessage());
    }

    @Test
    void validateOriginalUrl_whenOriginalUrlIsSameHostWithBase_throwIllegalArgumentException() {
        shortUrlMappings = new ShortUrlMappings("https://example.com");
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> shortUrlMappings.validateOriginalUrl("http://example.com"));
        assertEquals("Prohibited domain: example.com", ex.getMessage());
    }
}
