package com.diepnn.shortenurl.service;

import com.diepnn.shortenurl.common.enums.UrlInfoStatus;
import com.diepnn.shortenurl.dto.UserInfo;
import com.diepnn.shortenurl.dto.cache.UrlInfoCache;
import com.diepnn.shortenurl.dto.request.UrlInfoRequest;
import com.diepnn.shortenurl.entity.UrlInfo;
import com.diepnn.shortenurl.exception.AliasAlreadyExistsException;
import com.diepnn.shortenurl.exception.NotFoundException;
import com.diepnn.shortenurl.repository.UrlInfoRepository;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestInstance(TestInstance.Lifecycle.PER_METHOD)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@Transactional
public class UrlInfoServiceImplIT {
    @Autowired
    private CacheManager cacheManager;

    private UrlInfoCache mockUrlInfoCache;
    private String validShortCode;

    @BeforeEach
    void setUp() {
        validShortCode = "test123";
        mockUrlInfoCache = new UrlInfoCache(
                1L,
                "https://test.com"
        );

        // Clear cache before each test
        cacheManager.getCache("urls").clear();
    }

    private UserInfo mockUserInfo(String ipAddress, String userAgent) {
        return new UserInfo(ipAddress, userAgent, LocalDateTime.now(), null);
    }

    @Nested
    class FindShortUrl {
        @MockitoBean
        private UrlInfoRepository urlInfoRepository;
        @MockitoBean
        private ShortCodeService shortCodeService;

        @Autowired
        private UrlInfoService urlService;

        @Test
        void findByShortCodeCache_FirstCall_CallsRepositoryAndCachesResult() {
            // Given
            when(urlInfoRepository.findUrlInfoCacheByShortCode(validShortCode)).thenReturn(Optional.of(mockUrlInfoCache));

            // When - First call
            UrlInfoCache result1 = urlService.findByShortCodeCache(validShortCode);

            // Then
            assertNotNull(result1);
            verify(urlInfoRepository, times(1)).findUrlInfoCacheByShortCode(validShortCode);

            // Verify cache contains the result
            assertNotNull(cacheManager.getCache("urls").get(validShortCode));
        }

        @Test
        void findByShortCodeCache_SecondCall_ReturnsCachedResultWithoutRepositoryCall() {
            // Given
            when(urlInfoRepository.findUrlInfoCacheByShortCode(validShortCode)).thenReturn(Optional.of(mockUrlInfoCache));

            // When - First call to populate cache
            UrlInfoCache result1 = urlService.findByShortCodeCache(validShortCode);

            // Second call should use cache
            UrlInfoCache result2 = urlService.findByShortCodeCache(validShortCode);

            // Then
            assertNotNull(result1);
            assertNotNull(result2);
            assertEquals(result1.originalUrl(), result2.originalUrl());

            // Repository should only be called once (first call)
            verify(urlInfoRepository, times(1)).findUrlInfoCacheByShortCode(validShortCode);
        }

        @Test
        void findByShortCodeCache_CacheEviction_CallsRepositoryAgain() {
            // Given
            when(urlInfoRepository.findUrlInfoCacheByShortCode(validShortCode))
                    .thenReturn(Optional.of(mockUrlInfoCache));

            // When - First call
            urlService.findByShortCodeCache(validShortCode);

            // Evict cache manually
            cacheManager.getCache("urls").evict(validShortCode);

            // Second call after cache eviction
            urlService.findByShortCodeCache(validShortCode);

            // Then - Repository should be called twice
            verify(urlInfoRepository, times(2)).findUrlInfoCacheByShortCode(validShortCode);
        }

        @Test
        void findByShortCodeCache_DifferentShortCodes_EachCallsRepository() {
            // Given
            String shortCode1 = "test1";
            String shortCode2 = "test2";

            UrlInfoCache cache1 = new UrlInfoCache(1L, "https://test1.com");
            UrlInfoCache cache2 = new UrlInfoCache(2L, "https://test2.com");

            when(urlInfoRepository.findUrlInfoCacheByShortCode(shortCode1)).thenReturn(Optional.of(cache1));
            when(urlInfoRepository.findUrlInfoCacheByShortCode(shortCode2)).thenReturn(Optional.of(cache2));

            // When
            UrlInfoCache result1 = urlService.findByShortCodeCache(shortCode1);
            UrlInfoCache result2 = urlService.findByShortCodeCache(shortCode2);

            // Then
            assertNotNull(result1);
            assertNotNull(result2);

            verify(urlInfoRepository, times(1)).findUrlInfoCacheByShortCode(shortCode1);
            verify(urlInfoRepository, times(1)).findUrlInfoCacheByShortCode(shortCode2);
        }

        @Test
        void findByShortCodeCache_whenShortCodeIsNotExisted_returnNotFoundException() {
            assertThrows(NotFoundException.class, () -> urlService.findByShortCodeCache("xyz"));
        }

        @Test
        void findByShortCodeCache_whenShortCodeIsNullOrBlank_throwIllegalArgumentException() {
            assertThrows(IllegalArgumentException.class, () -> urlService.findByShortCodeCache(null));
            assertThrows(IllegalArgumentException.class, () -> urlService.findByShortCodeCache(" "));
        }
    }

    @Nested
    @DisplayName("POST /api/v1/url-infos/create")
    class CreateUrlInfo {
        @Autowired
        private UrlInfoRepository urlInfoRepository;

        @Autowired
        private UrlInfoServiceImpl urlInfoService;

        @BeforeEach
        void setUp() {
            UrlInfo urlInfo = UrlInfo.builder()
                                     .id(123456789L)
                                     .shortCode("abcde")
                                     .originalUrl("https://google.com")
                                     .status(UrlInfoStatus.ACTIVE)
                                     .alias(true)
                                     .createdDatetime(LocalDateTime.now())
                                     .build();
            urlInfoRepository.saveAndFlush(urlInfo);
        }

        @Test
        void create_ShortCodeIsExisted_ThrowsException() {
            // Given
            UserInfo userInfo = mockUserInfo("127.0.0.1", "Mozilla/5.0");
            UrlInfoRequest userRequest = new UrlInfoRequest("https://netflix.com", "abcde");
            String expectedMessage = String.format("The alias '%s' is already in use.", userRequest.getAlias());

            // When
            AliasAlreadyExistsException ex = assertThrows(AliasAlreadyExistsException.class,
                                                          () -> urlInfoService.create(userRequest, userInfo));

            // Then
            assertEquals(expectedMessage, ex.getMessage());
        }
    }
}
