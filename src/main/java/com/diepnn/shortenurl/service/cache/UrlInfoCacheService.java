package com.diepnn.shortenurl.service.cache;

import com.diepnn.shortenurl.dto.UrlInfoDTO;
import com.diepnn.shortenurl.dto.cache.UrlInfoCache;
import com.diepnn.shortenurl.mapper.UrlInfoMapper;
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
    private final UrlInfoMapper urlInfoMapper;

    /**
     * Find a URL by its short code in the cache.
     *
     * @param shortCode the short code to look up
     * @return the URL info cache object, or null if not found
     */
    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "url-access", key = "#shortCode", unless = "#result == null")
    public UrlInfoCache findByShortCodeCache(String shortCode) {
        log.debug("Loading URL from database for short code: {}", shortCode);
        return urlInfoRepository.findUrlInfoCacheByShortCode(shortCode);
    }

    /**
     * Find all URLs for the given user ID.
     *
     * @param userId the user ID to look up
     * @return a list of URL info DTOs, or an empty list if not found
     */
    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "user-urls", key = "#userId", unless = "#result.isEmpty()")
    public List<UrlInfoDTO> findAllByUserId(Long userId) {
        log.debug("Loading URLs from database for user ID: {}", userId);
        return urlInfoMapper.toDtos(urlInfoRepository.findAllByUserIdOrderByCreatedDatetimeDesc(userId));
    }

    /**
     * Evict the cache for the given user ID.
     *
     * @param userId the user ID to evict the cache
     */
    @CacheEvict(cacheNames = "user-urls", key = "#userId")
    public void evictUserUrlsCache(Long userId) {
        log.debug("Evicted user-urls cache for user: {}", userId);
    }

    /**
     * Evict the cache for the given short code.
     *
     * @param shortCode the short code to evict the cache
     */
    @CacheEvict(cacheNames = "url-access", key = "#shortCode")
    public void evictUrlAccessCache(String shortCode) {
        log.debug("Evicted url-access cache for short code: {}", shortCode);
    }
}
