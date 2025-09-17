package com.diepnn.shortenurl.service;

import com.diepnn.shortenurl.exception.TooManyRequestException;

/**
 * Service for generating short codes
 */
public interface ShortCodeService {
    String generateShortCode(long id);
    String generateShortCode() throws TooManyRequestException;
    Long generateId() throws TooManyRequestException;
}
