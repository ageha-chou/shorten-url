package com.diepnn.shortenurl.service;

import com.diepnn.shortenurl.common.annotation.Admin;
import com.diepnn.shortenurl.common.annotation.validation.User;
import com.diepnn.shortenurl.common.enums.UrlInfoStatus;
import com.diepnn.shortenurl.dto.UrlInfoDTO;
import com.diepnn.shortenurl.dto.UserInfo;
import com.diepnn.shortenurl.dto.cache.UrlInfoCache;
import com.diepnn.shortenurl.dto.filter.UrlInfoFilter;
import com.diepnn.shortenurl.dto.request.DeactivateUrlInfo;
import com.diepnn.shortenurl.dto.request.UpdateOriginalUrl;
import com.diepnn.shortenurl.dto.request.UrlInfoRequest;
import com.diepnn.shortenurl.entity.UrlInfo;
import com.diepnn.shortenurl.exception.AliasAlreadyExistsException;
import com.diepnn.shortenurl.exception.IdCollisionException;
import com.diepnn.shortenurl.exception.NotFoundException;
import com.diepnn.shortenurl.mapper.UrlInfoMapper;
import com.diepnn.shortenurl.repository.UrlInfoRepository;
import com.diepnn.shortenurl.security.CustomUserDetails;
import com.diepnn.shortenurl.service.cache.UrlInfoCacheService;
import com.diepnn.shortenurl.utils.DateUtils;
import com.diepnn.shortenurl.utils.SqlConstraintUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Database-backed implementation of {@link UrlInfoService}
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UrlInfoServiceImpl extends BaseService implements UrlInfoService {
    private final UrlInfoRepository urlInfoRepository;
    private final ShortCodeService shortCodeService;
    private final UrlInfoMapper urlInfoMapper;
    private final UrlInfoCacheService urlInfoCacheService;

    @Transactional
    @Override
    public UrlInfoDTO create(UrlInfoRequest userRequest, UserInfo userInfo, Long userId) {
        long id = shortCodeService.generateId();

        boolean hasCustomAlias = StringUtils.isNotBlank(userRequest.getAlias());
        String shortCode = hasCustomAlias
                           ? userRequest.getAlias().toLowerCase()
                           : shortCodeService.generateShortCode(id);

        UrlInfo urlInfo = UrlInfo.builder()
                                 .id(id)
                                 .originalUrl(userRequest.getOriginalUrl())
                                 .status(UrlInfoStatus.ACTIVE)
                                 .alias(hasCustomAlias)
                                 .shortCode(shortCode)
                                 .userId(userId)
                                 .createdByIp(userInfo.ipAddress())
                                 .createdByUserAgent(userInfo.userAgent())
                                 .createdDatetime(DateUtils.nowTruncatedToSeconds())
                                 .build();

        try {
            UrlInfoDTO result = urlInfoMapper.toDto(urlInfoRepository.saveAndFlush(urlInfo));

            // Evict caches after successful creation
            if (userId != null) {
                urlInfoCacheService.evictUserUrlsCache(userId);
            }

            return result;
        } catch (DataIntegrityViolationException e) {
            if (SqlConstraintUtils.isPrimaryKeyViolation(e, null)) {
                throw new IdCollisionException("Id collision detected for id: " + id);
            }

            if (SqlConstraintUtils.isUniqueConstraintViolation(e, "uidx_url_info_original_url")) {
                throw new AliasAlreadyExistsException("The alias '" + shortCode + "' is already in use.");
            }

            throw e;
        }
    }

    @Transactional(readOnly = true)
    @Override
    public UrlInfoCache findByShortCodeCache(String shortCode) {
        if (StringUtils.isBlank(shortCode)) {
            throw new IllegalArgumentException("Short code cannot be null or empty");
        }

        UrlInfoCache urlInfoCache = urlInfoCacheService.findByShortCodeCache(shortCode);
        if (urlInfoCache == null) {
            throw new NotFoundException("Not found URL for short code: " + shortCode);
        }

        return urlInfoCache;
    }

    /**
     * Update last access time asynchronously
     */
    @Async("urlAccessExecutor")
    @Transactional
    @Override
    public void updateLastAccessDatetimeByIdAsync(Long urlId, LocalDateTime lastAccessDatetime) {
        log.debug("Updating last access time for URL ID: {} at {}", urlId, lastAccessDatetime);
        urlInfoRepository.updateLastAccessDatetimeById(urlId, lastAccessDatetime);
    }

    @Transactional(readOnly = true)
    @Override
    public List<UrlInfoDTO> findAllByUserId(Long userId) {
        if (userId == null) {
            throw new IllegalArgumentException("User id cannot be null");
        }

        List<UrlInfoDTO> urls = urlInfoCacheService.findAllByUserId(userId);
        if (CollectionUtils.isEmpty(urls)) {
            throw new NotFoundException("Not found URL for user id: " + userId);
        }

        return Collections.unmodifiableList(urls);
    }

    @Transactional
    @Override
    public UrlInfoDTO updateOriginalUrl(Long urlId, UpdateOriginalUrl userRequest, CustomUserDetails userDetails) {
        UrlInfo urlInfo = findById(urlId);

        Long ownerId = urlInfo.getUserId();
        Long currentUserId = userDetails.getId();
        if (notBelongToCurrentUser(ownerId, currentUserId)) {
            throw new AccessDeniedException("URL not belongs to current user");
        }

        urlInfo.setOriginalUrl(userRequest.getOriginalUrl());
        urlInfo.setUpdatedDatetime(DateUtils.nowTruncatedToSeconds());
        urlInfoRepository.save(urlInfo);
        urlInfoCacheService.evictUserUrlsCache(currentUserId);
        urlInfoCacheService.evictUrlAccessCache(urlInfo.getShortCode());
        return urlInfoMapper.toDto(urlInfo);
    }

    @Transactional
    @Override
    public void delete(Long urlId, CustomUserDetails userDetails) {
        UrlInfo urlInfo = findById(urlId);

        Long ownerId = urlInfo.getUserId();
        Long currentUserId = userDetails.getId();
        if (notBelongToCurrentUser(ownerId, currentUserId)) {
            throw new AccessDeniedException("URL not belongs to current user");
        }

        urlInfoRepository.deleteByIdAndUserId(urlId, userDetails.getId());
        urlInfoCacheService.evictUserUrlsCache(userDetails.getId());
        urlInfoCacheService.evictUrlAccessCache(urlInfo.getShortCode());
    }

    @Transactional
    @Override
    @User
    public void deactivateUrlInfo(Long urlId, CustomUserDetails userDetails) {
        UrlInfo urlInfo = findById(urlId);
        if (notBelongToCurrentUser(urlInfo.getUserId(), userDetails.getId())) {
            throw new AccessDeniedException("URL not belongs to current user");
        }

        //evict all caches
        urlInfoCacheService.evictUrlAccessCache(urlInfo.getShortCode());
        urlInfoCacheService.evictUserUrlsCache(urlInfo.getUserId());

        String deactivatedReason = String.format("Deactivated by %s", userDetails.getUsername());
        urlInfoRepository.deactivateUrlInfo(urlId, deactivatedReason, userDetails.getId());
    }

    @Transactional(readOnly = true)
    @Override
    @Admin
    public Page<UrlInfoDTO> findAllUrl(UrlInfoFilter filter, Pageable pageable) {
        Page<UrlInfo> page = urlInfoRepository.findAll(filter, pageable);

        if (page.isEmpty()) {
            log.debug("Not found URL for filter {}", filter);
            throw new NotFoundException("URL not found");
        }

        return page.map(urlInfoMapper::toDto);
    }

    @Transactional
    @Override
    @Admin
    public void deactivateUrlInfo(Long urlId, DeactivateUrlInfo userRequest, CustomUserDetails userDetails) {
        UrlInfo urlInfo = findById(urlId);

        //evict all caches
        urlInfoCacheService.evictUrlAccessCache(urlInfo.getShortCode());
        urlInfoCacheService.evictUserUrlsCache(urlInfo.getUserId());

        urlInfoRepository.deactivateUrlInfo(urlId, userRequest.getDeactivatedReason(), userDetails.getId());
    }


    private UrlInfo findById(Long urlId) {
        return urlInfoRepository.findById(urlId)
                                .orElseThrow(() -> new NotFoundException("URL not found"));
    }
}
