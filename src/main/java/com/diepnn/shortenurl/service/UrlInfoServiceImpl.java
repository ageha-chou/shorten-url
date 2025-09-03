package com.diepnn.shortenurl.service;

import org.springframework.stereotype.Service;

import com.diepnn.shortenurl.dto.request.UrlInfoRequest;
import com.diepnn.shortenurl.entity.UrlInfo;

/**
 * Database-backed implementation of {@link UrlInfoService}
 */
@Service
public class UrlInfoServiceImpl implements UrlInfoService {
    @Override
    public UrlInfo create(UrlInfoRequest userRequest) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'create'");
    }

    @Override
    public UrlInfo resolve(String shortUrl) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'resolve'");
    }
}
