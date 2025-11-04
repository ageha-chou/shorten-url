package com.diepnn.shortenurl.service;

import com.diepnn.shortenurl.common.enums.UrlInfoStatus;
import com.diepnn.shortenurl.common.enums.UserRole;
import com.diepnn.shortenurl.common.enums.UsersStatus;
import com.diepnn.shortenurl.dto.UrlInfoDTO;
import com.diepnn.shortenurl.dto.UserInfo;
import com.diepnn.shortenurl.dto.cache.UrlInfoCache;
import com.diepnn.shortenurl.dto.filter.UrlInfoFilter;
import com.diepnn.shortenurl.dto.request.DeactivateUrlInfo;
import com.diepnn.shortenurl.dto.request.UpdateOriginalUrl;
import com.diepnn.shortenurl.dto.request.UrlInfoRequest;
import com.diepnn.shortenurl.entity.UrlInfo;
import com.diepnn.shortenurl.entity.Users;
import com.diepnn.shortenurl.exception.AliasAlreadyExistsException;
import com.diepnn.shortenurl.exception.NotFoundException;
import com.diepnn.shortenurl.helper.BaseIntegrationTest;
import com.diepnn.shortenurl.helper.WithMockAdmin;
import com.diepnn.shortenurl.helper.WithMockRegularUser;
import com.diepnn.shortenurl.repository.UrlInfoRepository;
import com.diepnn.shortenurl.repository.UsersRepository;
import com.diepnn.shortenurl.security.CustomUserDetails;
import com.diepnn.shortenurl.service.cache.UrlInfoCacheService;
import com.diepnn.shortenurl.utils.DateUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest
public class UrlInfoServiceImplIT extends BaseIntegrationTest {
    @Autowired
    private CacheManager cacheManager;

    private UrlInfoCache mockUrlInfoCache;
    private String validShortCode;
    @Autowired
    private UrlInfoCacheService urlInfoCacheService;

    @BeforeEach
    void setUp() {
        validShortCode = "test123";
        mockUrlInfoCache = new UrlInfoCache(
                1L,
                "https://test.com"
        );
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

    @Nested
    class FindAllUrlTests {
        @Autowired
        private UrlInfoService urlInfoService;

        @Autowired
        private UrlInfoRepository urlInfoRepository;

        @Autowired
        private UsersRepository userRepository;

        @Value("${app.short-base-url}")
        private String shortBaseUrl;

        private Users testUser;
        private Users adminUser;
        private UrlInfo urlInfo1;
        private UrlInfo urlInfo2;
        private UrlInfo urlInfo3;

        @BeforeEach
        void setUp() {
            // Create test users
            testUser = Users.builder()
                            .email("user@test.com")
                            .password("password123")
                            .role(UserRole.USER)
                            .status(UsersStatus.ACTIVE)
                            .createdDatetime(LocalDateTime.now())
                            .build();
            testUser = userRepository.save(testUser);

            adminUser = Users.builder()
                             .email("admin@test.com")
                             .password("admin123")
                             .role(UserRole.ADMIN)
                             .status(UsersStatus.ACTIVE)
                             .createdDatetime(LocalDateTime.now())
                             .build();
            adminUser = userRepository.save(adminUser);

            // Create test URL infos
            urlInfo1 = UrlInfo.builder()
                              .id(1L)
                              .shortCode("abc123")
                              .originalUrl("https://example1.com")
                              .userId(testUser.getId())
                              .status(UrlInfoStatus.ACTIVE)
                              .createdDatetime(LocalDateTime.now().minusDays(1))
                              .build();
            urlInfo1 = urlInfoRepository.save(urlInfo1);

            urlInfo2 = UrlInfo.builder()
                              .id(2L)
                              .shortCode("xyz789")
                              .originalUrl("https://example2.com")
                              .userId(testUser.getId())
                              .status(UrlInfoStatus.ACTIVE)
                              .createdDatetime(LocalDateTime.now())
                              .build();
            urlInfo2 = urlInfoRepository.save(urlInfo2);

            urlInfo3 = UrlInfo.builder()
                              .id(3L)
                              .shortCode("def456")
                              .originalUrl("https://example3.com")
                              .userId(testUser.getId())
                              .status(UrlInfoStatus.ACTIVE)
                              .createdDatetime(LocalDateTime.now().plusDays(1))
                              .build();
            urlInfo3 = urlInfoRepository.save(urlInfo3);
        }

        @Test
        @WithMockAdmin
        @DisplayName("Should return all URLs with pagination")
        void withPagination_ShouldReturnCorrectPage() {
            UrlInfoFilter filter = new UrlInfoFilter(null, null, null, null, null, null, null);
            Pageable pageable = PageRequest.of(0, 2, Sort.by(Sort.Direction.DESC, "createdDatetime"));

            Page<UrlInfoDTO> result = urlInfoService.findAllUrl(filter, pageable);

            assertNotNull(result);
            assertEquals(2, result.getSize());
            assertEquals(3, result.getTotalElements());
            assertEquals(2, result.getTotalPages());
            assertEquals(0, result.getNumber());
            assertEquals(2, result.getContent().size());

            // Verify content is mapped correctly
            List<String> shortCodes = result.getContent().stream()
                                            .map(x -> x.getShortUrl().replace(shortBaseUrl + "/", ""))
                                            .toList();

            assertTrue(shortCodes.contains("xyz789") || shortCodes.contains("def456"),
                       "Expected result content to contain at least one of the short codes: xyz789, def456"
            );

            //get next page
            pageable = result.nextPageable();
            result = urlInfoService.findAllUrl(filter, pageable);
            assertNotNull(result);
            assertEquals(2, result.getSize());
            assertEquals(3, result.getTotalElements());
            assertEquals(2, result.getTotalPages());
            assertEquals(1, result.getNumber());
            assertEquals(1, result.getContent().size());

            shortCodes = result.getContent().stream()
                               .map(x -> x.getShortUrl().replace(shortBaseUrl + "/", ""))
                               .toList();

            assertTrue(shortCodes.contains("abc123"),
                       "Expected result content to contain at least one of the short codes: abc123"
            );
        }

        @Test
        @WithMockAdmin
        @DisplayName("Should return second page correctly")
        void secondPage_ShouldReturnRemainingUrls() {
            UrlInfoFilter filter = new UrlInfoFilter(null, null, null, null, null, null, null);
            Pageable pageable = PageRequest.of(1, 2);

            Page<UrlInfoDTO> result = urlInfoService.findAllUrl(filter, pageable);

            assertNotNull(result);
            assertEquals(1, result.getContent().size());
            assertEquals(1, result.getNumber());
            assertTrue(result.isLast());
        }

        @Test
        @WithMockAdmin
        @DisplayName("Should filter by userId")
        void withUserIdFilter_ShouldReturnFilteredResults() {
            // Given
            // Create another user with different URLs
            Users anotherUser = Users.builder()
                                     .email("another@test.com")
                                     .password("password")
                                     .role(UserRole.USER)
                                     .status(UsersStatus.ACTIVE)
                                     .createdDatetime(LocalDateTime.now())
                                     .build();
            anotherUser = userRepository.save(anotherUser);

            UrlInfo anotherUrlInfo = UrlInfo.builder()
                                            .id(4L)
                                            .shortCode("another123")
                                            .originalUrl("https://another.com")
                                            .userId(anotherUser.getId())
                                            .status(UrlInfoStatus.ACTIVE)
                                            .createdDatetime(LocalDateTime.now())
                                            .build();
            urlInfoRepository.save(anotherUrlInfo);

            UrlInfoFilter filter = new UrlInfoFilter(null, null, null, null, Set.of(testUser.getId()),
                                                     null, null);
            Pageable pageable = PageRequest.of(0, 10);

            Page<UrlInfoDTO> result = urlInfoService.findAllUrl(filter, pageable);

            assertEquals(3, result.getTotalElements());
            List<String> shortCodes = result.getContent().stream()
                                            .map(x -> x.getShortUrl().replace(shortBaseUrl + "/", ""))
                                            .toList();

            assertTrue(shortCodes.contains("abc123") || shortCodes.contains("xyz789") || shortCodes.contains("def456"),
                       "Expected result content to contain at least one of the short codes: abc123, xyz789, def456"
            );

            assertFalse(shortCodes.contains("another123"), "Expected result content to not contain short code: another123");
        }

        @Test
        @WithMockAdmin
        @DisplayName("Should throw NotFoundException when no URLs found")
        void whenNoUrlsFound_ShouldThrowNotFoundException() {
            urlInfoRepository.deleteAll();
            UrlInfoFilter filter = new UrlInfoFilter(null, null, null, null, null, null, null);
            Pageable pageable = PageRequest.of(0, 10);

            NotFoundException ex = assertThrows(NotFoundException.class, () -> urlInfoService.findAllUrl(filter, pageable));
            assertEquals("URL not found", ex.getMessage());
        }

        @Test
        @WithMockAdmin
        @DisplayName("Should filter by ACTIVE status")
        void withActiveFilter_ShouldReturnOnlyActiveUrls() {
            // Deactivate one URL manually
            urlInfo1.setStatus(UrlInfoStatus.DEACTIVATE);
            urlInfoRepository.saveAndFlush(urlInfo1);

            UrlInfoFilter filter = new UrlInfoFilter(EnumSet.of(UrlInfoStatus.ACTIVE), null, null, null, null, null, null);
            Pageable pageable = PageRequest.of(0, 10);

            Page<UrlInfoDTO> result = urlInfoService.findAllUrl(filter, pageable);

            assertEquals(2, result.getTotalElements());
            List<String> shortCodes = result.getContent().stream()
                                            .map(x -> x.getShortUrl().replace(shortBaseUrl + "/", ""))
                                            .toList();

            assertTrue(shortCodes.contains("xyz789") || shortCodes.contains("def456"),
                       "Expected result content to contain at least one of the short codes: xyz789, def456"
            );

            assertFalse(shortCodes.contains("abc123"), "Expected result content to not contain short code: abc123");
        }

        @Test
        @WithMockAdmin
        @DisplayName("Should handle empty page correctly")
        void withPageBeyondData_ShouldThrowNotFoundException() {
            UrlInfoFilter filter = new UrlInfoFilter(null, null, null, null, null, null, null);
            Pageable pageable = PageRequest.of(10, 10); // Page way beyond available data

            NotFoundException ex = assertThrows(NotFoundException.class, () -> urlInfoService.findAllUrl(filter, pageable));
            assertEquals("URL not found", ex.getMessage());
        }

        @Test
        @WithMockAdmin
        @DisplayName("Admin user should have access")
        void withAdminRole_ShouldAllowAccess() {
            UrlInfoFilter filter = new UrlInfoFilter(null, null, null, null, null, null, null);
            Pageable pageable = PageRequest.of(0, 10);

            // No exception should be thrown
            Page<UrlInfoDTO> page = urlInfoService.findAllUrl(filter, pageable);
            assertNotNull(page);
            assertFalse(page.getContent().isEmpty(), "Page should not be empty");
        }

        @Test
        @WithMockUser
        @DisplayName("Regular user should be denied access")
        void withUserRole_ShouldDenyAccess() {
            UrlInfoFilter filter = new UrlInfoFilter(null, null, null, null, null, null, null);
            Pageable pageable = PageRequest.of(0, 10);

            assertThrows(AccessDeniedException.class, () -> urlInfoService.findAllUrl(filter, pageable));
        }

        @Test
        @DisplayName("Unauthenticated user should be denied access")
        void withoutAuthentication_ShouldDenyAccess() {
            UrlInfoFilter filter = new UrlInfoFilter(null, null, null, null, null, null, null);

            Pageable pageable = PageRequest.of(0, 10);
            assertThrows(AuthenticationCredentialsNotFoundException.class, () -> urlInfoService.findAllUrl(filter, pageable));
        }
    }

    @Nested
    class DeactivateUrlInfoTests {
        @Autowired
        private UrlInfoService urlInfoService;

        @Autowired
        private UrlInfoRepository urlInfoRepository;

        @Autowired
        private UsersRepository userRepository;

        @Value("${app.short-base-url}")
        private String shortBaseUrl;

        private Users testUser;
        private Users adminUser;
        private UrlInfo urlInfo1;
        private UrlInfo urlInfo2;
        private UrlInfo urlInfo3;
        private CustomUserDetails adminUserDetails;

        @BeforeEach
        void setUp() {
            // Create test users
            testUser = Users.builder()
                            .email("user@test.com")
                            .password("password123")
                            .role(UserRole.USER)
                            .status(UsersStatus.ACTIVE)
                            .createdDatetime(LocalDateTime.now())
                            .build();
            testUser = userRepository.save(testUser);

            adminUser = Users.builder()
                             .email("admin@test.com")
                             .password("admin123")
                             .role(UserRole.ADMIN)
                             .status(UsersStatus.ACTIVE)
                             .createdDatetime(LocalDateTime.now())
                             .build();
            adminUser = userRepository.save(adminUser);

            // Create test URL infos
            urlInfo1 = UrlInfo.builder()
                              .id(1L)
                              .shortCode("abc123")
                              .originalUrl("https://example1.com")
                              .userId(testUser.getId())
                              .status(UrlInfoStatus.ACTIVE)
                              .createdDatetime(LocalDateTime.now().minusDays(1))
                              .build();
            urlInfo1 = urlInfoRepository.save(urlInfo1);

            urlInfo2 = UrlInfo.builder()
                              .id(2L)
                              .shortCode("xyz789")
                              .originalUrl("https://example2.com")
                              .userId(testUser.getId())
                              .status(UrlInfoStatus.ACTIVE)
                              .createdDatetime(LocalDateTime.now())
                              .build();
            urlInfo2 = urlInfoRepository.save(urlInfo2);

            urlInfo3 = UrlInfo.builder()
                              .id(3L)
                              .shortCode("def456")
                              .originalUrl("https://example3.com")
                              .userId(testUser.getId())
                              .status(UrlInfoStatus.ACTIVE)
                              .createdDatetime(LocalDateTime.now().plusDays(1))
                              .build();
            urlInfo3 = urlInfoRepository.save(urlInfo3);

            adminUserDetails = CustomUserDetails.create(adminUser);
        }

        @Test
        @WithMockAdmin
        @DisplayName("Should deactivate URL and persist changes")
        void shouldDeactivateAndPersist() {
            Long urlId = urlInfo1.getId();
            String reason = "Spam content detected";
            DeactivateUrlInfo request = new DeactivateUrlInfo(reason);

            urlInfoService.deactivateUrlInfo(urlId, request, adminUserDetails);

            UrlInfo deactivatedUrl = urlInfoRepository.findById(urlId).orElseThrow();
            assertEquals(UrlInfoStatus.DEACTIVATE, deactivatedUrl.getStatus(), "URL should be marked as deactivated");
            assertEquals(reason, deactivatedUrl.getDeactivatedReason());
            assertEquals(adminUser.getId(), deactivatedUrl.getDeactivatedBy());
            assertNotNull(deactivatedUrl.getDeactivatedDatetime());
        }

        @Test
        @WithMockAdmin
        @DisplayName("Should evict URL access cache")
        void shouldEvictUrlAccessCache() {
            Long urlId = urlInfo1.getId();
            String shortCode = urlInfo1.getShortCode();
            DeactivateUrlInfo request = new DeactivateUrlInfo("Policy violation");

            // Populate cache first
            urlInfoCacheService.findByShortCodeCache(shortCode);

            urlInfoService.deactivateUrlInfo(urlId, request, adminUserDetails);

            // Cache should be evicted
            if (cacheManager.getCache("url-access") != null) {
                assertNull(cacheManager.getCache("url-access").get(shortCode));
            }
        }

        @Test
        @WithMockAdmin
        @DisplayName("Should evict user URLs cache")
        void deactivateUrlInfo_ShouldEvictUserUrlsCache() {
            Long urlId = urlInfo1.getId();
            Long userId = urlInfo1.getUserId();
            DeactivateUrlInfo request = new DeactivateUrlInfo("Copyright infringement");

            // Populate cache first
            urlInfoCacheService.findByShortCodeCache(urlInfo1.getShortCode());

            urlInfoService.deactivateUrlInfo(urlId, request, adminUserDetails);

            // Cache should be evicted
            if (cacheManager != null && cacheManager.getCache("userUrls") != null) {
                assertNull(cacheManager.getCache("userUrls").get(userId));
            }
        }

        @Test
        @WithMockAdmin
        @DisplayName("Should throw NotFoundException when URL not found")
        void deactivateUrlInfo_WithInvalidUrlId_ShouldThrowNotFoundException() {
            Long invalidUrlId = 99999L;
            DeactivateUrlInfo request = new DeactivateUrlInfo("Test reason");

            // When & Then
            NotFoundException ex = assertThrows(NotFoundException.class,
                                                () -> urlInfoService.deactivateUrlInfo(invalidUrlId, request, adminUserDetails));
            assertEquals("URL not found", ex.getMessage());
        }

        @Test
        @WithMockAdmin
        @DisplayName("Should not affect other URLs")
        void shouldNotAffectOtherUrls() {
            Long urlId = urlInfo1.getId();
            DeactivateUrlInfo request = new DeactivateUrlInfo("Deactivating first URL");

            urlInfoService.deactivateUrlInfo(urlId, request, adminUserDetails);

            // Then
            UrlInfo deactivatedUrl = urlInfoRepository.findById(urlInfo1.getId()).orElseThrow();
            UrlInfo otherUrl2 = urlInfoRepository.findById(urlInfo2.getId()).orElseThrow();
            UrlInfo otherUrl3 = urlInfoRepository.findById(urlInfo3.getId()).orElseThrow();

            assertEquals(UrlInfoStatus.DEACTIVATE, deactivatedUrl.getStatus());
            assertEquals(UrlInfoStatus.ACTIVE, otherUrl2.getStatus());
            assertEquals(UrlInfoStatus.ACTIVE, otherUrl3.getStatus());
        }

        @Test
        @WithMockAdmin
        @DisplayName("Should record correct admin who deactivated")
        void shouldRecordCorrectAdmin() {
            Long urlId = urlInfo1.getId();
            DeactivateUrlInfo request = new DeactivateUrlInfo("Admin action");

            urlInfoService.deactivateUrlInfo(urlId, request, adminUserDetails);

            UrlInfo deactivatedUrl = urlInfoRepository.findById(urlId).orElseThrow();
            assertEquals(adminUser.getId(), deactivatedUrl.getDeactivatedBy());
        }

        @Test
        @WithMockAdmin
        @DisplayName("Should not allow deactivating already deactivated URL")
        void alreadyDeactivated_ShouldUpdateReason() {
            Long urlId = urlInfo1.getId();
            DeactivateUrlInfo firstRequest = new DeactivateUrlInfo("First reason");
            DeactivateUrlInfo secondRequest = new DeactivateUrlInfo("Updated reason");

            // Deactivate first time
            urlInfoService.deactivateUrlInfo(urlId, firstRequest, adminUserDetails);

            // Deactivate second time
            urlInfoService.deactivateUrlInfo(urlId, secondRequest, adminUserDetails);

            UrlInfo deactivatedUrl = urlInfoRepository.findById(urlId).orElseThrow();
            assertEquals(UrlInfoStatus.DEACTIVATE, deactivatedUrl.getStatus());
            assertEquals(firstRequest.getDeactivatedReason(), deactivatedUrl.getDeactivatedReason());
        }

        @Test
        @WithMockAdmin
        @DisplayName("Integration - Deactivate URL then verify it doesn't appear in active filter")
        void shouldNotAppearInActiveFilter() {
            Long urlId = urlInfo1.getId();
            DeactivateUrlInfo request = new DeactivateUrlInfo("Test deactivation");

            urlInfoService.deactivateUrlInfo(urlId, request, adminUserDetails);

            UrlInfoFilter filter = new UrlInfoFilter(EnumSet.of(UrlInfoStatus.ACTIVE), null, null, null,
                                                     null, null, null);
            Pageable pageable = PageRequest.of(0, 10);

            Page<UrlInfoDTO> result = urlInfoService.findAllUrl(filter, pageable);

            assertEquals(2, result.getTotalElements());
            List<String> shortCodes = result.getContent()
                                            .stream().map(x -> x.getShortUrl().replace(shortBaseUrl + "/", ""))
                                            .toList();

            assertFalse(shortCodes.contains(urlInfo1.getShortCode()),
                        "Expected result content to not contain short code: " + urlInfo1.getShortCode());
        }

        @Test
        @WithMockAdmin
        @DisplayName("Integration - Deactivate all URLs then findAll should throw exception")
        void deactivateAllUrlsThenFindAll_ShouldThrowNotFoundException() {
            DeactivateUrlInfo request = new DeactivateUrlInfo("Mass deactivation");

            urlInfoService.deactivateUrlInfo(urlInfo1.getId(), request, adminUserDetails);
            urlInfoService.deactivateUrlInfo(urlInfo2.getId(), request, adminUserDetails);
            urlInfoService.deactivateUrlInfo(urlInfo3.getId(), request, adminUserDetails);

            UrlInfoFilter filter = new UrlInfoFilter(EnumSet.of(UrlInfoStatus.ACTIVE), null, null, null, null, null, null);
            Pageable pageable = PageRequest.of(0, 10);

            NotFoundException ex = assertThrows(NotFoundException.class, () -> urlInfoService.findAllUrl(filter, pageable));
            assertEquals("URL not found", ex.getMessage());
        }

        @Test
        @WithMockRegularUser
        @DisplayName("Regular user should be denied access")
        void withUserRole_ShouldDenyAccess() {
            Long urlId = urlInfo1.getId();
            CustomUserDetails regularUserDetails = CustomUserDetails.create(testUser);
            DeactivateUrlInfo request = new DeactivateUrlInfo("Test deactivation");

            assertThrows(AccessDeniedException.class, () -> urlInfoService.deactivateUrlInfo(urlId, request, regularUserDetails));

            // Verify URL was NOT deactivated
            UrlInfo notDeactivated = urlInfoRepository.findById(urlId).orElseThrow();
            assertEquals(UrlInfoStatus.ACTIVE, notDeactivated.getStatus());
        }

        @Test
        @WithMockUser(roles = {"USER", "MODERATOR", "EDITOR"})
        @DisplayName("User with multiple non-admin roles should be denied")
        void withMultipleNonAdminRoles_ShouldDenyAccess() {
            Long urlId = urlInfo1.getId();
            CustomUserDetails regularUserDetails = CustomUserDetails.create(testUser);
            DeactivateUrlInfo request = new DeactivateUrlInfo("Test deactivation");

            assertThrows(AccessDeniedException.class, () -> urlInfoService.deactivateUrlInfo(urlId, request, regularUserDetails));
        }

        @Test
        @DisplayName("Unauthenticated user should be denied access")
        void withoutAuthentication_ShouldDenyAccess() {
            Long urlId = urlInfo1.getId();
            DeactivateUrlInfo request = new DeactivateUrlInfo("Test deactivation");

            assertThrows(AuthenticationCredentialsNotFoundException.class, () -> urlInfoService.deactivateUrlInfo(urlId, request, null));
        }
    }
}
