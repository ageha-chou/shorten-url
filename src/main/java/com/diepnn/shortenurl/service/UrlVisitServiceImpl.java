package com.diepnn.shortenurl.service;

import com.diepnn.shortenurl.dto.UserInfo;
import com.diepnn.shortenurl.entity.UrlInfo;
import com.diepnn.shortenurl.entity.UrlVisit;
import com.diepnn.shortenurl.mapper.UrlVisitMapper;
import com.diepnn.shortenurl.repository.UrlVisitRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

@Service
@RequiredArgsConstructor
public class UrlVisitServiceImpl implements UrlVisitService {
    private final UrlVisitRepository urlVisitRepository;
    private final UrlVisitMapper urlVisitMapper;

    /**
     * Create a log when the short URL is visited.
     *
     * @param shortUrl the short url is accessed
     * @param userInfo user information
     * @return the log record
     * @throws IllegalArgumentException if either argument is null
     */
    @Transactional
    @Override
    public UrlVisit create(UrlInfo shortUrl, UserInfo userInfo) {
        if (shortUrl == null) {
            throw new IllegalArgumentException("URL info cannot be null");
        }

        if (userInfo == null) {
            throw new IllegalArgumentException("User info cannot be null");
        }

        UrlVisit urlVisit = urlVisitMapper.toEntity(userInfo);
        urlVisit.setShortenUrl(shortUrl);
        return urlVisitRepository.save(urlVisit);
    }

    @Async
    @Override
    public Future<UrlVisit> createAsync(UrlInfo shortUrl, UserInfo userInfo) {
        if (shortUrl == null) {
            throw new IllegalArgumentException("URL info cannot be null");
        }

        if (userInfo == null) {
            throw new IllegalArgumentException("User info cannot be null");
        }

        UrlVisit urlVisit = urlVisitMapper.toEntity(userInfo);
        urlVisit.setShortenUrl(shortUrl);
        return CompletableFuture.supplyAsync(() -> urlVisitRepository.save(urlVisit));
    }
}