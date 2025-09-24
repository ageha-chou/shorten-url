package com.diepnn.shortenurl.service;

import com.diepnn.shortenurl.common.enums.UrlInfoStatus;
import com.diepnn.shortenurl.dto.UrlInfoDTO;
import com.diepnn.shortenurl.dto.UserInfo;
import com.diepnn.shortenurl.dto.request.UrlInfoRequest;
import com.diepnn.shortenurl.entity.UrlInfo;
import com.diepnn.shortenurl.exception.AliasAlreadyExistsException;
import com.diepnn.shortenurl.exception.IdCollisionException;
import com.diepnn.shortenurl.mapper.UrlInfoMapper;
import com.diepnn.shortenurl.repository.UrlInfoRepository;
import com.diepnn.shortenurl.utils.SqlConstraintUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class UrlInfoServiceImplTests {
    @Mock
    private ShortCodeService shortCodeService;

    @Mock
    private UrlInfoRepository urlInfoRepository;

    @Mock
    private UrlInfoMapper urlInfoMapper;

    @InjectMocks
    private UrlInfoServiceImpl urlService;

    private UrlInfoRequest mockRequest;
    private UserInfo mockUserInfo;
    private UrlInfo mockUrlInfo;
    private UrlInfoDTO mockDto;
    private long mockId;
    private Long userId;

    @BeforeEach
    void setUp() {
        mockId = 12345L;
        userId = 123L;
        mockRequest = new UrlInfoRequest("https://example.com", "customAlias");
        mockUserInfo = new UserInfo("192.168.1.1", "Mozilla/5.0", LocalDateTime.now(), null);

        mockUrlInfo = UrlInfo.builder()
                             .id(mockId)
                             .originalUrl("https://example.com")
                             .status(UrlInfoStatus.ACTIVE)
                             .alias(true)
                             .shortCode("customalias")
                             .userId(userId)
                             .createdByIp("192.168.1.1")
                             .createdByUserAgent("Mozilla/5.0")
                             .createdDatetime(LocalDateTime.now())
                             .build();

        mockDto = UrlInfoDTO.builder()
                            .id(mockId)
                            .originalUrl("https://example.com")
                            .status("A")
                            .alias(true)
                            .shortUrl("http://localhost:8080/customalias")
                            .createdDatetime(LocalDateTime.now())
                            .build();
    }

    @Test
    void create_ValidRequestWithCustomAlias_ReturnsCreatedUrlInfo() {
        // Given
        when(shortCodeService.generateId()).thenReturn(mockId);
        when(urlInfoRepository.saveAndFlush(any(UrlInfo.class))).thenReturn(mockUrlInfo);
        when(urlInfoMapper.toDto(any(UrlInfo.class))).thenReturn(mockDto);

        // When
        UrlInfoDTO result = urlService.create(mockRequest, mockUserInfo, userId);

        // Then
        assertNotNull(result);
        assertEquals(mockId, result.getId());
        assertEquals("https://example.com", result.getOriginalUrl());
        assertEquals("http://localhost:8080/customalias", result.getShortUrl()); // lowercase
        assertEquals(UrlInfoStatus.ACTIVE.getValue(), result.getStatus());
        assertTrue(result.getAlias());

        verify(shortCodeService, times(1)).generateId();
        verify(shortCodeService, never()).generateShortCode(anyLong()); // Should not generate shortCode when alias provided
        verify(urlInfoRepository, times(1)).saveAndFlush(any(UrlInfo.class));
    }

    @Test
    void create_ValidRequestWithoutAlias_GeneratesShortCodeAndReturnsUrlInfo() {
        // Given
        UrlInfoRequest requestWithoutAlias = new UrlInfoRequest("https://example.com", null);

        String generatedShortCode = "abc123";
        when(shortCodeService.generateId()).thenReturn(mockId);
        when(shortCodeService.generateShortCode(mockId)).thenReturn(generatedShortCode);

        UrlInfo expectedUrlInfo = UrlInfo.builder()
                                         .id(mockId)
                                         .originalUrl("https://example.com")
                                         .status(UrlInfoStatus.ACTIVE)
                                         .alias(false)
                                         .shortCode(generatedShortCode)
                                         .createdByIp("192.168.1.1")
                                         .createdByUserAgent("Mozilla/5.0")
                                         .createdDatetime(LocalDateTime.now())
                                         .build();
        UrlInfoDTO expectedDto = UrlInfoDTO.builder()
                                           .id(mockId)
                                           .originalUrl("https://example.com")
                                           .status(UrlInfoStatus.ACTIVE.getValue())
                                           .alias(false)
                                           .shortUrl("http://localhost:8080/" + generatedShortCode)
                                           .createdDatetime(LocalDateTime.now())
                                           .build();

        when(urlInfoRepository.saveAndFlush(any(UrlInfo.class))).thenReturn(expectedUrlInfo);
        when(urlInfoMapper.toDto(expectedUrlInfo)).thenReturn(expectedDto);

        // When
        UrlInfoDTO result = urlService.create(requestWithoutAlias, mockUserInfo, userId);

        // Then
        assertNotNull(result);
        assertEquals(mockId, result.getId());
        assertEquals("http://localhost:8080/" + generatedShortCode, result.getShortUrl());
        assertFalse(result.getAlias()); // Should be false when no alias provided

        verify(shortCodeService, times(1)).generateId();
        verify(shortCodeService, times(1)).generateShortCode(mockId);
        verify(urlInfoRepository, times(1)).saveAndFlush(any(UrlInfo.class));
    }

    @Test
    void create_BlankAlias_GeneratesShortCodeInstead() {
        // Given
        UrlInfoRequest requestWithBlankAlias = new UrlInfoRequest("https://example.com", "  ");

        String generatedShortCode = "def456";
        when(shortCodeService.generateId()).thenReturn(mockId);
        when(shortCodeService.generateShortCode(mockId)).thenReturn(generatedShortCode);

        UrlInfo urlInfo = UrlInfo.builder()
                                 .id(mockId)
                                 .originalUrl("https://example.com")
                                 .status(UrlInfoStatus.ACTIVE)
                                 .alias(false)
                                 .shortCode(generatedShortCode)
                                 .createdByIp("192.168.1.1")
                                 .createdByUserAgent("Mozilla/5.0")
                                 .createdDatetime(LocalDateTime.now())
                                 .build();

        UrlInfoDTO expectedDto = UrlInfoDTO.builder()
                                           .id(mockId)
                                           .originalUrl("https://example.com")
                                           .status("A")
                                           .alias(false)
                                           .shortUrl("http://localhost:8080/" + generatedShortCode)
                                           .createdDatetime(LocalDateTime.now())
                                           .build();

        when(urlInfoRepository.saveAndFlush(any(UrlInfo.class))).thenReturn(urlInfo);
        when(urlInfoMapper.toDto(urlInfo)).thenReturn(expectedDto);

        // When
        UrlInfoDTO result = urlService.create(requestWithBlankAlias, mockUserInfo, userId);

        // Then
        assertNotNull(result);
        assertEquals("http://localhost:8080/" + generatedShortCode, result.getShortUrl());
        assertFalse(result.getAlias());

        verify(shortCodeService, times(1)).generateShortCode(mockId);
        verify(urlInfoRepository, times(1)).saveAndFlush(any(UrlInfo.class));
    }

    @Test
    void create_PrimaryKeyViolation_ThrowsIdCollisionException() {
        // Given
        when(shortCodeService.generateId()).thenReturn(mockId);

        DataIntegrityViolationException primaryKeyException = new DataIntegrityViolationException("PK violation");
        when(urlInfoRepository.saveAndFlush(any(UrlInfo.class))).thenThrow(primaryKeyException);

        // Mock static method call
        try (var mockedStatic = mockStatic(SqlConstraintUtils.class)) {
            mockedStatic.when(() -> SqlConstraintUtils.isPrimaryKeyViolation(primaryKeyException, null))
                        .thenReturn(true);

            // When & Then
            IdCollisionException exception = assertThrows(IdCollisionException.class, () ->
                    urlService.create(mockRequest, mockUserInfo, userId));

            assertEquals("Id collision detected for id: " + mockId, exception.getMessage());
            verify(urlInfoRepository, times(1)).saveAndFlush(any(UrlInfo.class));
        }
    }

    @Test
    void create_UniqueConstraintViolation_ThrowsAliasAlreadyExistsException() {
        // Given
        when(shortCodeService.generateId()).thenReturn(mockId);

        DataIntegrityViolationException uniqueConstraintException = new DataIntegrityViolationException("Unique violation");
        when(urlInfoRepository.saveAndFlush(any(UrlInfo.class))).thenThrow(uniqueConstraintException);

        try (var mockedStatic = mockStatic(SqlConstraintUtils.class)) {
            mockedStatic.when(() -> SqlConstraintUtils.isPrimaryKeyViolation(uniqueConstraintException, null))
                        .thenReturn(false);
            mockedStatic.when(() -> SqlConstraintUtils.isUniqueConstraintViolation(uniqueConstraintException, "uidx_url_info_original_url"))
                        .thenReturn(true);

            // When & Then
            AliasAlreadyExistsException exception = assertThrows(AliasAlreadyExistsException.class, () ->
                    urlService.create(mockRequest, mockUserInfo, userId));

            assertEquals("The alias 'customalias' is already in use.", exception.getMessage());
            verify(urlInfoRepository, times(1)).saveAndFlush(any(UrlInfo.class));
        }
    }

    @Test
    void create_OtherDataIntegrityViolation_PropagatesOriginalException() {
        // Given
        when(shortCodeService.generateId()).thenReturn(mockId);

        DataIntegrityViolationException otherException = new DataIntegrityViolationException("Other DB error");
        when(urlInfoRepository.saveAndFlush(any(UrlInfo.class))).thenThrow(otherException);

        try (var mockedStatic = mockStatic(SqlConstraintUtils.class)) {
            mockedStatic.when(() -> SqlConstraintUtils.isPrimaryKeyViolation(otherException, null))
                        .thenReturn(false);
            mockedStatic.when(() -> SqlConstraintUtils.isUniqueConstraintViolation(otherException, "uidx_url_info_original_url"))
                        .thenReturn(false);

            // When & Then
            DataIntegrityViolationException exception = assertThrows(DataIntegrityViolationException.class, () ->
                    urlService.create(mockRequest, mockUserInfo, userId));

            assertEquals("Other DB error", exception.getMessage());
            assertSame(otherException, exception);
            verify(urlInfoRepository, times(1)).saveAndFlush(any(UrlInfo.class));
        }
    }

    @Test
    void create_CaseSensitiveAlias_ConvertsToLowercase() {
        // Given
        UrlInfoRequest requestWithUppercaseAlias = new UrlInfoRequest("https://example.com", "UPPERCASE");

        when(shortCodeService.generateId()).thenReturn(mockId);
        when(urlInfoRepository.saveAndFlush(any(UrlInfo.class))).thenAnswer(invocation -> invocation.<UrlInfo>getArgument(0));

        // When
        urlService.create(requestWithUppercaseAlias, mockUserInfo, userId);

        // Then
        verify(urlInfoRepository).saveAndFlush(argThat(urlInfo -> "uppercase".equals(urlInfo.getShortCode())));
    }

    @Test
    void create_SetsCorrectTimestamp() {
        // Given
        LocalDateTime beforeCall = LocalDateTime.now().minusSeconds(1);
        when(shortCodeService.generateId()).thenReturn(mockId);
        when(urlInfoRepository.saveAndFlush(any(UrlInfo.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        urlService.create(mockRequest, mockUserInfo, userId);
        LocalDateTime afterCall = LocalDateTime.now().plusSeconds(1);

        // Then
        verify(urlInfoRepository).saveAndFlush(argThat(urlInfo -> {
            LocalDateTime createdTime = urlInfo.getCreatedDatetime();
            return createdTime.isAfter(beforeCall) && createdTime.isBefore(afterCall);
        }));
    }
}
