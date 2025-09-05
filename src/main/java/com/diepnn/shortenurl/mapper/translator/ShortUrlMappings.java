package com.diepnn.shortenurl.mapper.translator;

import com.diepnn.shortenurl.common.annotation.FromShortUrl;
import com.diepnn.shortenurl.common.annotation.ShortUrlTranslator;
import com.diepnn.shortenurl.common.annotation.ToShortUrl;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;

@Component
@ShortUrlTranslator
public class ShortUrlMappings {
    private final URI baseUri;

    public ShortUrlMappings(@Value("${app.short-base-url}") String baseUrl) {
        this.baseUri = URI.create(baseUrl);
    }

    @ToShortUrl
    public String toShortUrl(String shortCode) {
        if (StringUtils.isBlank(shortCode)) {
            return null;
        }

        return UriComponentsBuilder.newInstance()
                                   .path(shortCode)
                                   .scheme(baseUri.getScheme())
                                   .host(baseUri.getHost())
                                   .port(baseUri.getPort())
                                   .toUriString();
    }

    @FromShortUrl
    public String fromShortUrl(String shortUrl) {
        if (StringUtils.isBlank(shortUrl)) {
            return null;
        }

        String rawPath = URI.create(shortUrl).getRawPath();
        int idx = rawPath.lastIndexOf('/');
        return idx >= 0 ? rawPath.substring(idx + 1) : rawPath;
    }

    /**
     * Normalize the given URL.
     *
     * @param originalUrl the original URL
     * @return the normalized URL
     */
    public String normalizeUrl(String originalUrl) {
        if (originalUrl.startsWith("http://") || originalUrl.startsWith("https://")) {
            return originalUrl;
        }

        return "http://" + originalUrl;
    }

    /**
     * Validate the given original URL.
     *
     * @param originalUrl the original URL to validate
     * @throws IllegalArgumentException if the URL is invalid
     */
    public void validateOriginalUrl(String originalUrl) {
        if (StringUtils.isBlank(originalUrl)) {
            throw new IllegalArgumentException("Invalid input: " + originalUrl);
        }

        URI uri = URI.create(originalUrl);
        if (baseUri.getHost().equals(StringUtils.lowerCase(uri.getHost()))) {
            throw new IllegalArgumentException("Prohibited domain: " + uri.getHost());
        }

        if (baseUri.getHost().equals(StringUtils.lowerCase(uri.getScheme()))) {
            throw new IllegalArgumentException("Prohibited domain: " + uri.getScheme());
        }
    }
}
