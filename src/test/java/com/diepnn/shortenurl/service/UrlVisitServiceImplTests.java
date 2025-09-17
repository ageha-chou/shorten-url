package com.diepnn.shortenurl.service;

import com.diepnn.shortenurl.dto.UserInfo;
import com.diepnn.shortenurl.entity.UrlInfo;
import com.diepnn.shortenurl.entity.UrlVisit;
import com.diepnn.shortenurl.mapper.UrlVisitMapper;
import com.diepnn.shortenurl.repository.UrlVisitRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class UrlVisitServiceImplTests {
    @Mock
    private UrlVisitRepository urlVisitRepository;

    @Mock
    private UrlVisitMapper urlVisitMapper;

    @InjectMocks
    private UrlVisitServiceImpl urlVisitServiceImpl;

    @Test
    public void create_whenUrlInfoAndUserInfoAreValid_returnUrlVisit() {
        UrlInfo urlInfo = new UrlInfo();
        UserInfo userInfo = new UserInfo("127.0.0.1", "Mozilla/5.0", LocalDateTime.now(), null);
        UrlVisit urlVisit = new UrlVisit();

        when(urlVisitMapper.toEntity(userInfo)).thenReturn(urlVisit);
        ArgumentCaptor<UrlVisit> arg = ArgumentCaptor.forClass(UrlVisit.class);

        urlVisitServiceImpl.create(urlInfo, userInfo);
        verify(urlVisitRepository).save(arg.capture());
        assertNotNull(arg.getValue().getShortenUrl(), "Shorten URL must not null");
    }

    @Test
    public void create_whenUrlInfoIsNull_throwException() {
        UserInfo userInfo = mock(UserInfo.class);
        assertThrows(IllegalArgumentException.class, () -> urlVisitServiceImpl.create(null, userInfo));
    }

    @Test
    public void create_whenUserInfoIsNull_throwException() {
        UrlInfo urlInfo = mock(UrlInfo.class);
        assertThrows(IllegalArgumentException.class, () -> urlVisitServiceImpl.create(urlInfo, null));
    }
}
