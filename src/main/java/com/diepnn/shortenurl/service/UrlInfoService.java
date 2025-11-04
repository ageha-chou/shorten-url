package com.diepnn.shortenurl.service;

import com.diepnn.shortenurl.dto.UrlInfoDTO;
import com.diepnn.shortenurl.dto.UserInfo;
import com.diepnn.shortenurl.dto.cache.UrlInfoCache;
import com.diepnn.shortenurl.dto.filter.UrlInfoFilter;
import com.diepnn.shortenurl.dto.request.DeactivateUrlInfo;
import com.diepnn.shortenurl.dto.request.UpdateOriginalUrl;
import com.diepnn.shortenurl.dto.request.UrlInfoRequest;
import com.diepnn.shortenurl.security.CustomUserDetails;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Service for shortening and resolving URLs
 */
public interface UrlInfoService {
    /**
     * Creates a short URL for the given original URL.
     *
     * @param userRequest contains the original URL and alias (optional)
     * @return a generated short URL
     */
    UrlInfoDTO create(UrlInfoRequest userRequest, UserInfo userInfo, Long userId);

    /**
     * Find url info by short code and cache it.
     * @param shortCode short code
     * @return url info
     */
    UrlInfoCache findByShortCodeCache(String shortCode);

    /**
     * Update last access datetime by id.
     * @param urlId url info id
     * @param lastAccessDatetime last access datetime
     */
    void updateLastAccessDatetimeByIdAsync(Long urlId, LocalDateTime lastAccessDatetime);

    /**
     * Find all url info by user id.
     *
     * @param userId authenticated user id
     * @return url info list
     */
    List<UrlInfoDTO> findAllByUserId(Long userId);

    /**
     * Update original url.
     *
     * @param urlId url info id
     * @param userRequest contains the original URL
     * @param userDetails user information
     * @return the updated url info
     */
    UrlInfoDTO updateOriginalUrl(Long urlId, UpdateOriginalUrl userRequest, CustomUserDetails userDetails);

    /**
     * Delete url info by id.
     *
     * @param urlId url info id
     * @param userDetails user information
     */
    void delete(Long urlId, CustomUserDetails userDetails);

    /**
     * Find all url info by filter.
     *
     * @param filter url info filter
     * @param pageable pageable object
     * @return url info list
     */
    Page<UrlInfoDTO> findAllUrl(UrlInfoFilter filter, Pageable pageable);

    /**
     * Deactivate url info by id used by {@code ADMIN} authority.
     *
     * @param urlId url info id
     * @param userRequest user request
     * @param userDetails user details
     */
    void deactivateUrlInfo(Long urlId, DeactivateUrlInfo userRequest, CustomUserDetails userDetails);

    /**
     * Deactivate url info by id used by {@code USER} authority.
     * Deny the request if the url info is not owned by the authenticated user.
     *
     * @param urlId url info id
     * @param userDetails user details
     */
    void deactivateUrlInfo(Long urlId, CustomUserDetails userDetails);
}
