package com.diepnn.shortenurl.service;

import com.diepnn.shortenurl.dto.UserInfo;
import com.diepnn.shortenurl.dto.request.UrlInfoRequest;
import com.diepnn.shortenurl.entity.UrlInfo;
import com.diepnn.shortenurl.exception.TooManyRequestException;

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
    UrlInfo create(UrlInfoRequest userRequest, UserInfo userInfo) throws TooManyRequestException;
    UrlInfo findByShortCode(String shortCode);
    void updateLastAccessDatetimeById(UrlInfo urlInfo);
}
