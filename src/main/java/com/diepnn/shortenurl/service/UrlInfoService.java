package com.diepnn.shortenurl.service;

import com.diepnn.shortenurl.dto.request.UrlInfoRequest;
import com.diepnn.shortenurl.entity.UrlInfo;

/**
 * Service for shortening and resolving URLs
 */
public interface UrlInfoService {
    /**
     * Creates a short URL for the given original URL.
     *
     * @param userRequest contains the original URL and alias (optional)
     * @return a generated short URL
     */
    UrlInfo create(UrlInfoRequest userRequest);

    /**
     * Resolves the given short URL to its original.
     *
     * @param shortUrl shorten URL
     * @return the original URL
     * @throws com.diepnn.shortenurl.exception.NotFoundException if the short URL does not exist
     */
    UrlInfo resolve(String shortUrl);
}
