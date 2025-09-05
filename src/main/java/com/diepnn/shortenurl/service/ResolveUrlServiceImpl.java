package com.diepnn.shortenurl.service;

import com.diepnn.shortenurl.dto.UserInfo;
import com.diepnn.shortenurl.entity.UrlInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ResolveUrlServiceImpl implements ResolveUrlService {
    private final UrlInfoService urlInfoService;
    private final UrlVisitService urlVisitService;

    @Transactional
    @Override
    public String resolve(String shortCode, UserInfo userInfo) {
        UrlInfo urlInfo = urlInfoService.findByShortCode(shortCode);
        urlVisitService.create(urlInfo, userInfo);

        //update last access datetime
        urlInfo.setLastAccessDatetime(userInfo.visitedDatetime());
        urlInfoService.updateLastAccessDatetimeById(urlInfo);

        return urlInfo.getOriginalUrl();
    }
}
