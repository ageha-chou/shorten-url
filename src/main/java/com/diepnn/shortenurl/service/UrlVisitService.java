package com.diepnn.shortenurl.service;

import com.diepnn.shortenurl.entity.UrlInfo;
import com.diepnn.shortenurl.entity.UrlVisit;

/**
 * Service for logging the visit to the short URL
 */
public interface UrlVisitService {
    /**
     * Create a log when the short URL is visited
     *
     * @param userAgent accessor's device information
     * @param ipAddress accessor's ip address
     * @param country accessor's country
     * @param shortUrl the short url is accessed
     */
    UrlVisit create(String userAgent, String ipAddress, String country, UrlInfo shortUrl);
}
