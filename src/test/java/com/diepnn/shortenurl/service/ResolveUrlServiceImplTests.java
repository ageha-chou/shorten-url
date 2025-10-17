package com.diepnn.shortenurl.service;

import com.diepnn.shortenurl.dto.UserInfo;
import com.diepnn.shortenurl.dto.cache.UrlInfoCache;
import com.diepnn.shortenurl.entity.UrlInfo;
import com.diepnn.shortenurl.utils.DateUtils;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ResolveUrlServiceImplTests {
    @Mock
    private UrlVisitService urlVisitService;

    @Mock
    private UrlInfoService urlInfoService;

    @Mock
    private EntityManager entityManager;

    @InjectMocks
    private ResolveUrlServiceImpl resolveUrlServiceImpl;

    private final String userIp = "192.168.1.1";
    private final String userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/140.0.0.0 Safari/537.36";
    private UserInfo userInfo;

    @BeforeEach
    public void setup() {
        userInfo = new UserInfo(userIp, userAgent, DateUtils.nowTruncatedToSeconds(), null);
    }

    @Test
    public void resolve_whenUrlInfoExists_returnUrlInfo() {
        String shortCode = "abc123";
        UrlInfoCache urlInfoCache = new UrlInfoCache(1L, "https://example.com");
        when(urlInfoService.findByShortCodeCache(shortCode)).thenReturn(urlInfoCache);
        when(entityManager.getReference(eq(UrlInfo.class), any(Long.class))).thenReturn(mock(UrlInfo.class));

        String url = resolveUrlServiceImpl.resolve(shortCode, userInfo);

        assertEquals(urlInfoCache.originalUrl(), url, "Resolved URL should match the mocked cache original URL");
        verify(urlInfoService).findByShortCodeCache(shortCode);
        verify(entityManager).getReference(eq(UrlInfo.class), any(Long.class));
        verify(urlVisitService).createAsync(any(UrlInfo.class), any(UserInfo.class));
        verify(urlInfoService).updateLastAccessDatetimeByIdAsync(any(Long.class), any(LocalDateTime.class));
    }
}
