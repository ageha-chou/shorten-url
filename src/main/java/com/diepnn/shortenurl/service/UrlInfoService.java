package com.diepnn.shortenurl.service;

import com.diepnn.shortenurl.dto.UrlInfoDTO;
import com.diepnn.shortenurl.dto.UserInfo;
import com.diepnn.shortenurl.dto.cache.UrlInfoCache;
import com.diepnn.shortenurl.dto.request.UrlInfoRequest;
import com.diepnn.shortenurl.exception.TooManyRequestException;

import java.time.LocalDateTime;

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
    UrlInfoDTO create(UrlInfoRequest userRequest, UserInfo userInfo, Long userId) throws TooManyRequestException;

    /**
     * Find url info by short code and cache it.
     * @param shortCode short code
     * @return url info
     */
    UrlInfoCache findByShortCodeCache(String shortCode);

    /**
     * Update last access datetime by id.
     * @param urlId url info id
     * @param lastAccessDatetime last access datetime
     */
    void updateLastAccessDatetimeById(Long urlId, LocalDateTime lastAccessDatetime);
}
