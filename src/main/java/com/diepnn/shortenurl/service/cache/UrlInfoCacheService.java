package com.diepnn.shortenurl.service.cache;

import com.diepnn.shortenurl.dto.UrlInfoDTO;
import com.diepnn.shortenurl.dto.cache.UrlInfoCache;
import com.diepnn.shortenurl.exception.NotFoundException;
import com.diepnn.shortenurl.repository.UrlInfoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Separate service for cache operations to avoid self-invocation issues
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UrlInfoCacheService {
    private final UrlInfoRepository urlInfoRepository;

    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "url-access", key = "#shortCode")
    public UrlInfoCache findByShortCodeCache(String shortCode) {
        log.debug("Loading URL from database for short code: {}", shortCode);
        return urlInfoRepository.findUrlInfoCacheByShortCode(shortCode)
                                .orElseThrow(() -> new NotFoundException("Not found URL: " + shortCode));
    }

    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "user-urls", key = "#userId", unless = "#result.isEmpty()")
    public List<UrlInfoDTO> findAllByUserId(Long userId) {
        log.debug("Loading URLs from database for user ID: {}", userId);
        return urlInfoRepository.findAllUrlInfoDtosByUserIdOrderByCreatedDatetimeDesc(userId);
    }

    @CacheEvict(cacheNames = "user-urls", key = "#userId")
    public void evictUserUrlsCache(Long userId) {
        log.debug("Evicted user-urls cache for user: {}", userId);
    }

    @CacheEvict(cacheNames = "url-access", key = "#shortCode")
    public void evictUrlAccessCache(String shortCode) {
        log.debug("Evicted url-access cache for short code: {}", shortCode);
    }
}
