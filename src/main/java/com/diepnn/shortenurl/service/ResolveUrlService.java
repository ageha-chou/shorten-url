package com.diepnn.shortenurl.service;

import com.diepnn.shortenurl.dto.UserInfo;
import com.diepnn.shortenurl.entity.UrlInfo;

public interface ResolveUrlService {
    /**
     * Resolves the given short URL to its original.
     *
     * @param shortCode shorten URL
     * @return the original URL
     * @throws com.diepnn.shortenurl.exception.NotFoundException if the short URL does not exist
     */
    String resolve(String shortCode, UserInfo userInfo);
}
