package com.diepnn.shortenurl.service;

import com.diepnn.shortenurl.common.enums.UrlInfoStatus;
import com.diepnn.shortenurl.dto.UserInfo;
import com.diepnn.shortenurl.dto.request.UrlInfoRequest;
import com.diepnn.shortenurl.entity.UrlInfo;
import com.diepnn.shortenurl.exception.AliasAlreadyExistsException;
import com.diepnn.shortenurl.exception.IdCollisionException;
import com.diepnn.shortenurl.exception.NotFoundException;
import com.diepnn.shortenurl.exception.TooManyRequestException;
import com.diepnn.shortenurl.repository.UrlInfoRepository;
import com.diepnn.shortenurl.utils.SqlConstraintUtils;
import com.diepnn.shortenurl.utils.UserInfoRequestExtractor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class UrlInfoServiceImplTests {
    @Mock
    private ShortCodeService shortCodeService;

    @Mock
    private UrlInfoRepository urlInfoRepository;

    @MockitoBean
    private UserInfoRequestExtractor userInfoRequestExtractor;

    @InjectMocks
    private UrlInfoServiceImpl urlInfoServiceImpl;

    @Captor
    private ArgumentCaptor<UrlInfo> urlInfoCaptor;

    private final String userIp = "192.168.1.1";
    private final String userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/140.0.0.0 Safari/537.36";
    private UserInfo userInfo;

    @BeforeEach
    public void setup() {
        userInfo = new UserInfo(userIp, userAgent, LocalDateTime.now(), null);
    }

    @Test
    void create_whenAliasIsNull_generateShortUrl() throws TooManyRequestException {
        UrlInfoRequest userRequest = new UrlInfoRequest("https://google.com", null);

        long id = 1L;
        String shortCode = "abc123";
        when(shortCodeService.generateId()).thenReturn(id);
        when(shortCodeService.generateShortCode(id)).thenReturn(shortCode);

        urlInfoServiceImpl.create(userRequest, userInfo);
        verify(shortCodeService).generateId();
        verify(shortCodeService).generateShortCode(id);
        verify(urlInfoRepository).saveAndFlush(urlInfoCaptor.capture());

        assertEquals(shortCode, urlInfoCaptor.getValue().getShortCode());
        assertFalse(urlInfoCaptor.getValue().getAlias());
        assertEquals(UrlInfoStatus.ACTIVE, urlInfoCaptor.getValue().getStatus());
        assertEquals(urlInfoCaptor.getValue().getCreatedByIp(), userIp);
        assertEquals(urlInfoCaptor.getValue().getCreatedByUserAgent(), userAgent);
    }

    @Test
    void create_whenAliasIsNotNull_notGenerateShortUrl() throws TooManyRequestException {
        UrlInfoRequest userRequest = new UrlInfoRequest("https://google.com", "abc123");

        long id = 1L;
        when(shortCodeService.generateId()).thenReturn(id);

        urlInfoServiceImpl.create(userRequest, userInfo);
        verify(shortCodeService).generateId();
        verify(shortCodeService, times(0)).generateShortCode(id);
        verify(urlInfoRepository).saveAndFlush(urlInfoCaptor.capture());

        assertEquals(userRequest.getAlias(), urlInfoCaptor.getValue().getShortCode());
        assertTrue(urlInfoCaptor.getValue().getAlias());
        assertEquals(UrlInfoStatus.ACTIVE, urlInfoCaptor.getValue().getStatus());
        assertEquals(urlInfoCaptor.getValue().getCreatedByIp(), userIp);
        assertEquals(urlInfoCaptor.getValue().getCreatedByUserAgent(), userAgent);
    }

    @Test
    void create_whenDuplicateAlias_throwAliasAlreadyExistException() throws TooManyRequestException {
        UrlInfoRequest userRequest = new UrlInfoRequest("https://google.com", "abc123");
        try (MockedStatic<SqlConstraintUtils> sqlConstraintUtilsMock = mockStatic(SqlConstraintUtils.class)) {
            sqlConstraintUtilsMock.when(() -> SqlConstraintUtils.isUniqueConstraintViolation(any(DataIntegrityViolationException.class),
                                                                                             any(String[].class)))
                                  .thenReturn(true);

            when(urlInfoRepository.saveAndFlush(any(UrlInfo.class))).thenThrow(DataIntegrityViolationException.class);
            when(shortCodeService.generateId()).thenReturn(1L);
            assertThrows(AliasAlreadyExistsException.class, () -> urlInfoServiceImpl.create(userRequest, userInfo));
        }
    }

    @Test
    void create_whenDuplicateId_throwIdCollisionException() throws TooManyRequestException {
        UrlInfoRequest userRequest = new UrlInfoRequest("https://google.com", null);
        try (MockedStatic<SqlConstraintUtils> sqlConstraintUtilsMock = mockStatic(SqlConstraintUtils.class)) {
            sqlConstraintUtilsMock.when(() -> SqlConstraintUtils.isPrimaryKeyViolation(any(DataIntegrityViolationException.class),
                                                                                       any()))
                                  .thenReturn(true);

            when(urlInfoRepository.saveAndFlush(any(UrlInfo.class))).thenThrow(DataIntegrityViolationException.class);
            when(shortCodeService.generateId()).thenReturn(1L);
            assertThrows(IdCollisionException.class, () -> urlInfoServiceImpl.create(userRequest, userInfo));
        }
    }

    @Test
    void findByShortCode_whenUrlInfoExists_returnUrlInfo() {
        String shortCode = "abc123";
        when(urlInfoRepository.findByShortCode(shortCode)).thenReturn(Optional.of(mock(UrlInfo.class)));

        UrlInfo urlInfo = urlInfoServiceImpl.findByShortCode(shortCode);
        assertNotNull(urlInfo, "UrlInfo must not null");
    }

    @Test
    void findByShortCode_whenUrlInfoNotExists_throwNotFoundException() {
        String shortCode = "abc123";
        when(urlInfoRepository.findByShortCode(shortCode)).thenReturn(Optional.empty());
        assertThrows(NotFoundException.class, () -> urlInfoServiceImpl.findByShortCode(shortCode));
    }
}
