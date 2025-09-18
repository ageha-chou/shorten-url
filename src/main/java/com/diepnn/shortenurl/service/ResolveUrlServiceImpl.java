package com.diepnn.shortenurl.service;

import com.diepnn.shortenurl.dto.cache.UrlInfoCache;
import com.diepnn.shortenurl.dto.UserInfo;
import com.diepnn.shortenurl.entity.UrlInfo;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ResolveUrlServiceImpl implements ResolveUrlService {
    private final UrlInfoService urlInfoService;
    private final UrlVisitService urlVisitService;
    private final EntityManager entityManager;

    @Transactional
    @Override
    public String resolve(String shortCode, UserInfo userInfo) {
        UrlInfoCache urlInfo = urlInfoService.findByShortCodeCache(shortCode);
        urlVisitService.create(entityManager.getReference(UrlInfo.class, urlInfo.id()), userInfo);
        urlInfoService.updateLastAccessDatetimeById(urlInfo.id(), userInfo.visitedDatetime());
        return urlInfo.originalUrl();
    }
}
