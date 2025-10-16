package com.diepnn.shortenurl.service;

import com.diepnn.shortenurl.common.enums.UrlInfoStatus;
import com.diepnn.shortenurl.dto.UrlInfoDTO;
import com.diepnn.shortenurl.dto.UserInfo;
import com.diepnn.shortenurl.dto.request.UpdateOriginalUrl;
import com.diepnn.shortenurl.dto.request.UrlInfoRequest;
import com.diepnn.shortenurl.entity.UrlInfo;
import com.diepnn.shortenurl.entity.Users;
import com.diepnn.shortenurl.exception.AliasAlreadyExistsException;
import com.diepnn.shortenurl.exception.IdCollisionException;
import com.diepnn.shortenurl.exception.NotFoundException;
import com.diepnn.shortenurl.mapper.UrlInfoMapper;
import com.diepnn.shortenurl.repository.UrlInfoRepository;
import com.diepnn.shortenurl.security.CustomUserDetails;
import com.diepnn.shortenurl.service.cache.UrlInfoCacheService;
import com.diepnn.shortenurl.utils.SqlConstraintUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doNothing;
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

    @Mock
    private UrlInfoCacheService urlInfoCacheService;

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

    @Nested
    @DisplayName("Test create function")
    class CreateUrlInfoTests {
        @Test
        void validRequestWithCustomAlias_ReturnsCreatedUrlInfo() {
            when(shortCodeService.generateId()).thenReturn(mockId);
            when(urlInfoRepository.saveAndFlush(any(UrlInfo.class))).thenReturn(mockUrlInfo);
            when(urlInfoMapper.toDto(any(UrlInfo.class))).thenReturn(mockDto);

            UrlInfoDTO result = urlService.create(mockRequest, mockUserInfo, null);

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
        void validRequestWithoutAlias_GeneratesShortCodeAndReturnsUrlInfo() {
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

            UrlInfoDTO result = urlService.create(requestWithoutAlias, mockUserInfo, null);

            assertNotNull(result);
            assertEquals(mockId, result.getId());
            assertEquals("http://localhost:8080/" + generatedShortCode, result.getShortUrl());
            assertFalse(result.getAlias()); // Should be false when no alias provided

            verify(shortCodeService, times(1)).generateId();
            verify(shortCodeService, times(1)).generateShortCode(mockId);
            verify(urlInfoRepository, times(1)).saveAndFlush(any(UrlInfo.class));
        }

        @Test
        void blankAlias_GeneratesShortCodeInstead() {
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
            doNothing().when(urlInfoCacheService).evictUserUrlsCache(userId);

            UrlInfoDTO result = urlService.create(requestWithBlankAlias, mockUserInfo, userId);

            assertNotNull(result);
            assertEquals("http://localhost:8080/" + generatedShortCode, result.getShortUrl());
            assertFalse(result.getAlias());

            verify(shortCodeService, times(1)).generateShortCode(mockId);
            verify(urlInfoRepository, times(1)).saveAndFlush(any(UrlInfo.class));
            verify(urlInfoCacheService, times(1)).evictUserUrlsCache(userId);
        }

        @Test
        void primaryKeyViolation_ThrowsIdCollisionException() {
            when(shortCodeService.generateId()).thenReturn(mockId);

            DataIntegrityViolationException primaryKeyException = new DataIntegrityViolationException("PK violation");
            when(urlInfoRepository.saveAndFlush(any(UrlInfo.class))).thenThrow(primaryKeyException);

            // Mock static method call
            try (var mockedStatic = mockStatic(SqlConstraintUtils.class)) {
                mockedStatic.when(() -> SqlConstraintUtils.isPrimaryKeyViolation(primaryKeyException, null))
                            .thenReturn(true);

                IdCollisionException exception = assertThrows(IdCollisionException.class, () ->
                        urlService.create(mockRequest, mockUserInfo, userId));

                assertEquals("Id collision detected for id: " + mockId, exception.getMessage());
                verify(urlInfoRepository, times(1)).saveAndFlush(any(UrlInfo.class));
            }
        }

        @Test
        void uniqueConstraintViolation_ThrowsAliasAlreadyExistsException() {
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
        void otherDataIntegrityViolation_PropagatesOriginalException() {
            when(shortCodeService.generateId()).thenReturn(mockId);

            DataIntegrityViolationException otherException = new DataIntegrityViolationException("Other DB error");
            when(urlInfoRepository.saveAndFlush(any(UrlInfo.class))).thenThrow(otherException);

            try (var mockedStatic = mockStatic(SqlConstraintUtils.class)) {
                mockedStatic.when(() -> SqlConstraintUtils.isPrimaryKeyViolation(otherException, null))
                            .thenReturn(false);
                mockedStatic.when(() -> SqlConstraintUtils.isUniqueConstraintViolation(otherException, "uidx_url_info_original_url"))
                            .thenReturn(false);

                DataIntegrityViolationException exception = assertThrows(DataIntegrityViolationException.class, () ->
                        urlService.create(mockRequest, mockUserInfo, userId));

                assertEquals("Other DB error", exception.getMessage());
                assertSame(otherException, exception);
                verify(urlInfoRepository, times(1)).saveAndFlush(any(UrlInfo.class));
            }
        }

        @Test
        void caseSensitiveAlias_ConvertsToLowercase() {
            UrlInfoRequest requestWithUppercaseAlias = new UrlInfoRequest("https://example.com", "UPPERCASE");

            when(shortCodeService.generateId()).thenReturn(mockId);
            when(urlInfoRepository.saveAndFlush(any(UrlInfo.class))).thenAnswer(invocation -> invocation.<UrlInfo>getArgument(0));

            urlService.create(requestWithUppercaseAlias, mockUserInfo, null);

            verify(urlInfoRepository).saveAndFlush(argThat(urlInfo -> "uppercase".equals(urlInfo.getShortCode())));
            verify(urlInfoCacheService, times(0)).evictUserUrlsCache(null);
        }

        @Test
        void setsCorrectTimestamp() {
            LocalDateTime beforeCall = LocalDateTime.now().minusSeconds(1);
            when(shortCodeService.generateId()).thenReturn(mockId);
            when(urlInfoRepository.saveAndFlush(any(UrlInfo.class))).thenAnswer(invocation -> invocation.getArgument(0));
            doNothing().when(urlInfoCacheService).evictUserUrlsCache(userId);

            urlService.create(mockRequest, mockUserInfo, userId);
            LocalDateTime afterCall = LocalDateTime.now().plusSeconds(1);

            verify(urlInfoRepository).saveAndFlush(argThat(urlInfo -> {
                LocalDateTime createdTime = urlInfo.getCreatedDatetime();
                return createdTime.isAfter(beforeCall) && createdTime.isBefore(afterCall);
            }));
            verify(urlInfoCacheService, times(1)).evictUserUrlsCache(userId);
        }
    }

    @Nested
    @DisplayName("Test findAllByUserId function")
    class FindAllByUserIdTests {
        @Test
        void whenUserIdIsNull_ThrowsIllegalArgumentException() {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> urlService.findAllByUserId(null));
            assertEquals("User id cannot be null", ex.getMessage());
        }

        @Test
        void whenUrlInfoNotFound_ThrowsNotFoundException() {
            when(urlInfoCacheService.findAllByUserId(anyLong())).thenReturn(Collections.emptyList());
            NotFoundException ex = assertThrows(NotFoundException.class, () -> urlService.findAllByUserId(mockId));
            assertEquals("Not found URL for user id: " + mockId, ex.getMessage());
        }

        @Test
        void whenUrlInfoFound_ReturnsList() {
            when(urlInfoCacheService.findAllByUserId(anyLong())).thenReturn(Collections.singletonList(mockDto));
            List<UrlInfoDTO> result = urlService.findAllByUserId(mockId);
            assertEquals(1, result.size());
            assertEquals(mockDto, result.getFirst());
        }
    }

    @Nested
    @DisplayName("Test updateOriginalUrl function")
    class UpdateOriginalUrlTests {
        private UpdateOriginalUrl userRequest;

        @BeforeEach
        void setUp() {
            userRequest = new UpdateOriginalUrl("https://updated-example.com");
        }

        @Test
        void whenUrlInfoNotFound_ThrowsNotFoundException() {
            when(urlInfoRepository.findById(anyLong())).thenReturn(Optional.empty());
            assertThrows(NotFoundException.class, () -> urlService.updateOriginalUrl(mockUrlInfo.getId(), userRequest, any()));
        }

        @Test
        void whenUrlNotBelongsToCurrentUser_ThrowsException() {
            CustomUserDetails userDetails = CustomUserDetails.create(Users.builder().id(2L).build());
            when(urlInfoRepository.findById(anyLong())).thenReturn(Optional.of(mockUrlInfo));
            assertThrows(IllegalArgumentException.class, () -> urlService.updateOriginalUrl(anyLong(), userRequest, userDetails));
        }

        @Test
        void whenFound_UpdatesOriginalUrl() {
            CustomUserDetails userDetails = CustomUserDetails.create(Users.builder().id(userId).build());
            when(urlInfoRepository.findById(anyLong())).thenReturn(Optional.of(mockUrlInfo));
            when(urlInfoRepository.save(any(UrlInfo.class))).thenAnswer(invocation -> invocation.getArgument(0));
            when(urlInfoMapper.toDto(any(UrlInfo.class))).thenReturn(mockDto);

            UrlInfoDTO result = urlService.updateOriginalUrl(mockUrlInfo.getId(), userRequest, userDetails);

            assertEquals(mockDto, result);
            verify(urlInfoRepository).save(argThat(urlInfo -> urlInfo.getOriginalUrl().equals("https://updated-example.com")));
            verify(urlInfoCacheService, times(1)).evictUserUrlsCache(userId);
            verify(urlInfoCacheService, times(1)).evictUrlAccessCache(mockUrlInfo.getShortCode());
        }
    }
}
