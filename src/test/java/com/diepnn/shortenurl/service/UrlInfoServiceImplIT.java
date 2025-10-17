package com.diepnn.shortenurl.service;

import com.diepnn.shortenurl.common.enums.UrlInfoStatus;
import com.diepnn.shortenurl.dto.UrlInfoDTO;
import com.diepnn.shortenurl.dto.UserInfo;
import com.diepnn.shortenurl.dto.cache.UrlInfoCache;
import com.diepnn.shortenurl.dto.request.UpdateOriginalUrl;
import com.diepnn.shortenurl.dto.request.UrlInfoRequest;
import com.diepnn.shortenurl.entity.UrlInfo;
import com.diepnn.shortenurl.entity.Users;
import com.diepnn.shortenurl.exception.AliasAlreadyExistsException;
import com.diepnn.shortenurl.exception.NotFoundException;
import com.diepnn.shortenurl.repository.UrlInfoRepository;
import com.diepnn.shortenurl.repository.UsersRepository;
import com.diepnn.shortenurl.security.CustomUserDetails;
import com.diepnn.shortenurl.service.cache.UrlInfoCacheService;
import com.diepnn.shortenurl.utils.DateUtils;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
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

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

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
        cacheManager.getCache("url-access").clear();
        Set<String> keys = redisTemplate.keys("authenticated-user::*");
        if (!keys.isEmpty()) {
            redisTemplate.delete(keys);
        }
    }

    private UserInfo mockUserInfo(String ipAddress, String userAgent) {
        return new UserInfo(ipAddress, userAgent, DateUtils.nowTruncatedToSeconds(), null);
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
        void firstCall_CallsRepositoryAndCachesResult() {
            // Given
            when(urlInfoRepository.findUrlInfoCacheByShortCode(validShortCode)).thenReturn(mockUrlInfoCache);

            // When - First call
            UrlInfoCache result1 = urlService.findByShortCodeCache(validShortCode);

            // Then
            assertNotNull(result1);
            verify(urlInfoRepository, times(1)).findUrlInfoCacheByShortCode(validShortCode);

            // Verify cache contains the result
            assertNotNull(cacheManager.getCache("url-access").get(validShortCode));
        }

        @Test
        void secondCall_ReturnsCachedResultWithoutRepositoryCall() {
            // Given
            when(urlInfoRepository.findUrlInfoCacheByShortCode(validShortCode)).thenReturn(mockUrlInfoCache);

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
        void cacheEviction_CallsRepositoryAgain() {
            // Given
            when(urlInfoRepository.findUrlInfoCacheByShortCode(validShortCode))
                    .thenReturn(mockUrlInfoCache);

            // When - First call
            urlService.findByShortCodeCache(validShortCode);

            // Evict cache manually
            cacheManager.getCache("url-access").evict(validShortCode);

            // Second call after cache eviction
            urlService.findByShortCodeCache(validShortCode);

            // Then - Repository should be called twice
            verify(urlInfoRepository, times(2)).findUrlInfoCacheByShortCode(validShortCode);
        }

        @Test
        void differentShortCodes_EachCallsRepository() {
            // Given
            String shortCode1 = "test1";
            String shortCode2 = "test2";

            UrlInfoCache cache1 = new UrlInfoCache(1L, "https://test1.com");
            UrlInfoCache cache2 = new UrlInfoCache(2L, "https://test2.com");

            when(urlInfoRepository.findUrlInfoCacheByShortCode(shortCode1)).thenReturn(cache1);
            when(urlInfoRepository.findUrlInfoCacheByShortCode(shortCode2)).thenReturn(cache2);

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
        void whenShortCodeIsNotExisted_returnNotFoundException() {
            assertThrows(NotFoundException.class, () -> urlService.findByShortCodeCache("xyz"));
        }

        @Test
        void whenShortCodeIsNullOrBlank_throwIllegalArgumentException() {
            assertThrows(IllegalArgumentException.class, () -> urlService.findByShortCodeCache(null));
            assertThrows(IllegalArgumentException.class, () -> urlService.findByShortCodeCache(" "));
        }
    }

    @Nested
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
                                     .createdDatetime(DateUtils.nowTruncatedToSeconds())
                                     .build();
            urlInfoRepository.saveAndFlush(urlInfo);
        }

        @Test
        void shortCodeIsExisted_ThrowsException() {
            UserInfo userInfo = mockUserInfo("127.0.0.1", "Mozilla/5.0");
            UrlInfoRequest userRequest = new UrlInfoRequest("https://netflix.com", "abcde");
            String expectedMessage = String.format("The alias '%s' is already in use.", userRequest.getAlias());
            Long userId = 1L;

            AliasAlreadyExistsException ex = assertThrows(AliasAlreadyExistsException.class,
                                                          () -> urlInfoService.create(userRequest, userInfo, userId));

            assertEquals(expectedMessage, ex.getMessage());
        }
    }

    @Nested
    class UpdateUrlInfo {
        @Autowired
        private UrlInfoService urlInfoService;

        @Autowired
        private UrlInfoRepository urlInfoRepository;

        @Autowired
        private UsersRepository usersRepository;

        @Autowired
        private UrlInfoCacheService urlInfoCacheService;

        private CustomUserDetails userDetails;
        private CustomUserDetails otherUserDetails;
        private UrlInfo testUrlInfo;

        @BeforeEach
        void setUp() {
            // Setup user details
            Users testuser = Users.builder().username("testuser").build();
            Users otheruser = Users.builder().username("otheruser").build();

            userDetails = CustomUserDetails.create(usersRepository.saveAndFlush(testuser));
            otherUserDetails = CustomUserDetails.create(usersRepository.saveAndFlush(otheruser));

            // Create test URL info
            testUrlInfo = new UrlInfo();
            testUrlInfo.setId(1000L);
            testUrlInfo.setUserId(userDetails.getId());
            testUrlInfo.setOriginalUrl("https://example.com/original");
            testUrlInfo.setShortCode("abc123");
            testUrlInfo.setStatus(UrlInfoStatus.ACTIVE);
            testUrlInfo.setCreatedDatetime(DateUtils.nowTruncatedToSeconds());
            testUrlInfo.setUpdatedDatetime(DateUtils.nowTruncatedToSeconds());
            testUrlInfo = urlInfoRepository.saveAndFlush(testUrlInfo);
        }

        @Test
        void shouldUpdateOriginalUrlSuccessfully() throws InterruptedException {
            String newOriginalUrl = "https://example.com/updated";
            UpdateOriginalUrl request = new UpdateOriginalUrl(newOriginalUrl);
            LocalDateTime beforeUpdate = DateUtils.nowTruncatedToSeconds();

            Thread.sleep(1000);

            UrlInfoDTO result = urlInfoService.updateOriginalUrl(testUrlInfo.getId(), request, userDetails);

            assertNotNull(result);
            assertEquals(newOriginalUrl, result.getOriginalUrl(), "Original URL should be updated");

            // Verify database update
            UrlInfo updatedUrlInfo = urlInfoRepository.findById(testUrlInfo.getId()).orElseThrow();
            assertEquals(newOriginalUrl, updatedUrlInfo.getOriginalUrl(), "Original URL in DB should be updated");
            assertTrue(updatedUrlInfo.getUpdatedDatetime().isAfter(beforeUpdate));
            assertEquals(userDetails.getId(), updatedUrlInfo.getUserId());
        }

        @Test
        void shouldThrowNotFoundExceptionWhenUrlDoesNotExist() {
            Long nonExistentUrlId = 99999L;
            UpdateOriginalUrl request = new UpdateOriginalUrl("https://example.com/new");

            NotFoundException ex = assertThrows(NotFoundException.class,
                                                () -> urlInfoService.updateOriginalUrl(nonExistentUrlId, request, userDetails));
            assertEquals("URL not found", ex.getMessage());
        }

        @Test
        void shouldThrowIllegalArgumentExceptionWhenUrlBelongsToDifferentUser() {
            UpdateOriginalUrl request = new UpdateOriginalUrl("https://example.com/new");

            AccessDeniedException ex = assertThrows(AccessDeniedException.class,
                                                    () -> urlInfoService.updateOriginalUrl(testUrlInfo.getId(), request, otherUserDetails));

            assertEquals("URL not belongs to current user", ex.getMessage());
        }

        @Test
        void shouldEvictCacheAfterUpdate() {
            String newOriginalUrl = "https://example.com/cache-test";
            UpdateOriginalUrl request = new UpdateOriginalUrl(newOriginalUrl);

            // Pre-populate cache by calling the cacheable methods
            urlInfoCacheService.findAllByUserId(userDetails.getId());
            urlInfoCacheService.findByShortCodeCache(testUrlInfo.getShortCode());

            urlInfoService.updateOriginalUrl(testUrlInfo.getId(), request, userDetails);

            // Verify cache was evicted by checking that subsequent calls fetch fresh data
            List<UrlInfoDTO> userUrls = urlInfoCacheService.findAllByUserId(userDetails.getId());
            UrlInfoCache urlCache = urlInfoCacheService.findByShortCodeCache(testUrlInfo.getShortCode());

            // Verify the data is fresh (contains the updated URL)
            assertTrue(userUrls.stream().anyMatch(url -> url.getId().equals(testUrlInfo.getId())));
            assertEquals(newOriginalUrl, urlCache.originalUrl(),
                         String.format("Cache should be update to %s but found %s", newOriginalUrl, urlCache.originalUrl()));
        }

        @Test
        void shouldUpdateTimestampCorrectly() {
            LocalDateTime originalTimestamp = testUrlInfo.getUpdatedDatetime();
            UpdateOriginalUrl request = new UpdateOriginalUrl("https://example.com/timestamp-test");

            // Small delay to ensure timestamp difference
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            urlInfoService.updateOriginalUrl(testUrlInfo.getId(), request, userDetails);

            UrlInfo updatedUrlInfo = urlInfoRepository.findById(testUrlInfo.getId()).orElseThrow();
            assertTrue(updatedUrlInfo.getUpdatedDatetime().isAfter(originalTimestamp));
        }

        @Test
        void shouldPreserveOtherFieldsWhenUpdating() {
            String originalShortCode = testUrlInfo.getShortCode();
            Long originalUserId = testUrlInfo.getUserId();
            LocalDateTime originalCreatedDatetime = testUrlInfo.getCreatedDatetime();

            UpdateOriginalUrl request = new UpdateOriginalUrl("https://example.com/preserve-test");

            urlInfoService.updateOriginalUrl(testUrlInfo.getId(), request, userDetails);

            UrlInfo updatedUrlInfo = urlInfoRepository.findById(testUrlInfo.getId()).orElseThrow();
            assertEquals(originalShortCode, updatedUrlInfo.getShortCode());
            assertEquals(originalUserId, updatedUrlInfo.getUserId());
            assertEquals(originalCreatedDatetime, updatedUrlInfo.getCreatedDatetime());
        }
    }

    @Nested
    class DeleteTests {
        @Autowired
        private UrlInfoService urlInfoService;

        @Autowired
        private UrlInfoRepository urlInfoRepository;

        @Autowired
        private UsersRepository usersRepository;

        @Autowired
        private UrlInfoCacheService urlInfoCacheService;

        private CustomUserDetails userDetails;
        private CustomUserDetails otherUserDetails;
        private UrlInfo testUrlInfo;

        @BeforeEach
        void setUp() {
            // Setup user details
            Users testuser = Users.builder().username("testuser").build();
            Users otheruser = Users.builder().username("otheruser").build();

            userDetails = CustomUserDetails.create(usersRepository.saveAndFlush(testuser));
            otherUserDetails = CustomUserDetails.create(usersRepository.saveAndFlush(otheruser));

            // Create test URL info
            testUrlInfo = new UrlInfo();
            testUrlInfo.setId(1000L);
            testUrlInfo.setUserId(userDetails.getId());
            testUrlInfo.setOriginalUrl("https://example.com/original");
            testUrlInfo.setShortCode("abc123");
            testUrlInfo.setStatus(UrlInfoStatus.ACTIVE);
            testUrlInfo.setCreatedDatetime(DateUtils.nowTruncatedToSeconds());
            testUrlInfo.setUpdatedDatetime(DateUtils.nowTruncatedToSeconds());
            testUrlInfo = urlInfoRepository.saveAndFlush(testUrlInfo);
        }

        @Test
        void whenUrlNotExist_throwNotFoundException() {
            Long nonExistentUrlId = 99999L;
            assertThrows(NotFoundException.class, () -> urlInfoService.delete(nonExistentUrlId, userDetails));
        }

        @Test
        void whenUrlBelongsToDifferentUser_throwAccessDeniedException() {
            assertThrows(AccessDeniedException.class, () -> urlInfoService.delete(testUrlInfo.getId(), otherUserDetails));
        }

        @Test
        void whenUrlBelongsToCurrentUser_deleteUrlInfo() {
            urlInfoService.delete(testUrlInfo.getId(), userDetails);
            Optional<UrlInfo> deletedUrlInfo = urlInfoRepository.findById(testUrlInfo.getId());
            assertTrue(deletedUrlInfo.isPresent(), "URL should be present");
            assertEquals(UrlInfoStatus.DELETED, deletedUrlInfo.get().getStatus(), "URL should be marked as deleted");
            assertNotNull(deletedUrlInfo.get().getDeletedDatetime(), "Deleted datetime should be set");
        }

        @Test
        void whenUrlInfoIsDeleted_evictCache() {
            // Pre-populate cache by calling the cacheable methods
            urlInfoCacheService.findAllByUserId(userDetails.getId());
            urlInfoCacheService.findByShortCodeCache(testUrlInfo.getShortCode());

            urlInfoService.delete(testUrlInfo.getId(), userDetails);

            List<UrlInfoDTO> userUrls = urlInfoCacheService.findAllByUserId(userDetails.getId());
            UrlInfoCache urlInfoCache = urlInfoCacheService.findByShortCodeCache(testUrlInfo.getShortCode());

            assertTrue(userUrls.stream().noneMatch(url -> url.getId().equals(testUrlInfo.getId())),
                       "URL should be removed from cache");
            assertNull(urlInfoCache, "Cache should be evicted");
        }
    }
}
