package com.diepnn.shortenurl.service;

/**
 * Service for generating short codes
 */
public interface ShortCodeService {
    String generateShortCode(long id);
    String generateShortCode();
    Long generateId();
}
