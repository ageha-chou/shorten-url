package com.diepnn.shortenurl.service;

import com.diepnn.shortenurl.common.enums.UrlInfoStatus;
import com.diepnn.shortenurl.dto.UserInfo;
import com.diepnn.shortenurl.dto.request.UrlInfoRequest;
import com.diepnn.shortenurl.entity.UrlInfo;
import com.diepnn.shortenurl.exception.AliasAlreadyExistsException;
import com.diepnn.shortenurl.exception.IdCollisionException;
import com.diepnn.shortenurl.exception.NotFoundException;
import com.diepnn.shortenurl.repository.UrlInfoRepository;
import com.diepnn.shortenurl.utils.SqlConstraintUtils;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Database-backed implementation of {@link UrlInfoService}
 */
@Service
@RequiredArgsConstructor
public class UrlInfoServiceImpl implements UrlInfoService {
    private final UrlInfoRepository urlInfoRepository;
    private final ShortCodeService shortCodeService;

    @Transactional
    @Override
    public UrlInfo create(UrlInfoRequest userRequest, UserInfo userInfo) {
        long id = shortCodeService.generateId();

        String shortCode = StringUtils.lowerCase(userRequest.getAlias());
        if (StringUtils.isBlank(shortCode)) {
            shortCode = shortCodeService.generateShortCode(id);
        }

        UrlInfo urlInfo = UrlInfo.builder()
                                 .id(id)
                                 .originalUrl(userRequest.getOriginalUrl())
                                 .status(UrlInfoStatus.ACTIVE)
                                 .alias(userRequest.getAlias() != null)
                                 .shortCode(shortCode)
                                 .createdByIp(userInfo.ipAddress())
                                 .createdByUserAgent(userInfo.userAgent())
                                 .createdDatetime(LocalDateTime.now())
                                 .build();

        try {
            return urlInfoRepository.saveAndFlush(urlInfo);
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
    public UrlInfo findByShortCode(String shortCode) {
        if (StringUtils.isBlank(shortCode)) {
            throw new IllegalArgumentException("Short code cannot be null or empty");
        }

        return urlInfoRepository.findByShortCode(shortCode)
                                .orElseThrow(() -> new NotFoundException("Not found URL: " + shortCode));
    }

    @Transactional
    @Override
    public void updateLastAccessDatetimeById(UrlInfo urlInfo) {
        urlInfoRepository.updateLastAccessDatetimeById(urlInfo.getId(), urlInfo.getLastAccessDatetime());
    }
}
