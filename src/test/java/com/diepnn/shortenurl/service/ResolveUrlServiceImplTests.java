package com.diepnn.shortenurl.service;

import com.diepnn.shortenurl.dto.UserInfo;
import com.diepnn.shortenurl.entity.UrlInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ResolveUrlServiceImplTests {
    @Mock
    private UrlVisitService urlVisitService;

    @Mock
    private UrlInfoService urlInfoService;

    @InjectMocks
    private ResolveUrlServiceImpl resolveUrlServiceImpl;

    private final String userIp = "192.168.1.1";
    private final String userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/140.0.0.0 Safari/537.36";
    private UserInfo userInfo;

    @BeforeEach
    public void setup() {
        userInfo = new UserInfo(userIp, userAgent, LocalDateTime.now(), null);
    }

    @Test
    public void resolve_whenUrlInfoExists_returnUrlInfo() {
        String shortCode = "abc123";
        when(urlInfoService.findByShortCode(shortCode)).thenReturn(mock(UrlInfo.class));

        resolveUrlServiceImpl.resolve(shortCode, userInfo);

        verify(urlInfoService).findByShortCode(shortCode);
        verify(urlVisitService).create(any(UrlInfo.class), any(UserInfo.class));
        verify(urlInfoService).updateLastAccessDatetimeById(any(UrlInfo.class));
    }
}
