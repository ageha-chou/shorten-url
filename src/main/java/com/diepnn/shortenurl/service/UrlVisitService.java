package com.diepnn.shortenurl.service;

import com.diepnn.shortenurl.dto.UserInfo;
import com.diepnn.shortenurl.entity.UrlInfo;
import com.diepnn.shortenurl.entity.UrlVisit;

/**
 * Service for logging the visit to the short URL
 */
public interface UrlVisitService {
    /**
     * Create a log when the short URL is visited
     *
     * @param shortUrl the short url is accessed
     * @param userInfo user information
     * @return the log record
     */
    UrlVisit create(UrlInfo shortUrl, UserInfo userInfo);
}
