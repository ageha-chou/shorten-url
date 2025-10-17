package com.diepnn.shortenurl.repository;

import com.diepnn.shortenurl.common.enums.UrlInfoStatus;
import com.diepnn.shortenurl.dto.cache.UrlInfoCache;
import com.diepnn.shortenurl.entity.UrlInfo;
import com.diepnn.shortenurl.entity.Users;
import com.diepnn.shortenurl.helper.RepositoryTest;
import com.diepnn.shortenurl.utils.DateUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@RepositoryTest
public class UrlInfoRepositoryTests {
    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private UrlInfoRepository urlInfoRepository;

    private Users testUser;
    private UrlInfo testUrlInfo1;
    private UrlInfo testUrlInfo2;
    private UrlInfo deletedUrlInfo;

    @BeforeEach
    void setUp() {
        // Create test data
        testUser = Users.builder()
                        .username("testuser")
                        .firstName("Test")
                        .lastName("User")
                        .createdDatetime(DateUtils.nowTruncatedToSeconds())
                        .build();

        testUser = entityManager.persist(testUser);

        testUrlInfo1 = UrlInfo.builder()
                              .id(1L)
                              .shortCode("abc123")
                              .originalUrl("https://example.com/page1")
                              .alias(false)
                              .userId(testUser.getId())
                              .status(UrlInfoStatus.ACTIVE)
                              .createdDatetime(DateUtils.nowTruncatedToSeconds().minusDays(2))
                              .lastAccessDatetime(DateUtils.nowTruncatedToSeconds().minusHours(1))
                              .build();

        testUrlInfo2 = UrlInfo.builder()
                              .id(2L)
                              .shortCode("xyz789")
                              .originalUrl("https://example.com/page2")
                              .alias(true)
                              .userId(testUser.getId())
                              .status(UrlInfoStatus.ACTIVE)
                              .createdDatetime(DateUtils.nowTruncatedToSeconds().minusDays(1))
                              .lastAccessDatetime(DateUtils.nowTruncatedToSeconds().minusMinutes(30))
                              .build();

        deletedUrlInfo = UrlInfo.builder()
                                .id(3L)
                                .shortCode("del456")
                                .alias(false)
                                .userId(testUser.getId())
                                .status(UrlInfoStatus.DELETED)
                                .createdDatetime(DateUtils.nowTruncatedToSeconds().minusDays(3))
                                .deletedDatetime(DateUtils.nowTruncatedToSeconds().minusDays(1))
                                .build();

        entityManager.persist(testUrlInfo1);
        entityManager.persist(testUrlInfo2);
        entityManager.persist(deletedUrlInfo);
        entityManager.flush();
    }

    @Test
    void findUrlInfoCacheByShortCode_WhenExists_ShouldReturnUrlInfoCache() {
        UrlInfoCache result = urlInfoRepository.findUrlInfoCacheByShortCode("abc123");

        assertNotNull(result);
        assertEquals("https://example.com/page1", result.originalUrl());
    }

    @Test
    void findUrlInfoCacheByShortCode_WhenNotExists_ShouldReturnEmpty() {
        UrlInfoCache result = urlInfoRepository.findUrlInfoCacheByShortCode("notexist");
        assertNull(result);
    }

    @Test
    void updateLastAccessDatetimeById_ShouldUpdateSuccessfully() {
        LocalDateTime newAccessTime = DateUtils.nowTruncatedToSeconds();
        Long urlId = testUrlInfo1.getId();

        urlInfoRepository.updateLastAccessDatetimeById(urlId, newAccessTime);
        entityManager.flush();
        entityManager.clear();

        UrlInfo updated = entityManager.find(UrlInfo.class, urlId);
        assertThat(updated.getLastAccessDatetime()).isEqualToIgnoringNanos(newAccessTime);
    }

    @Test
    void findAllByUserIdOrderByCreatedDatetimeDesc_ShouldReturnActiveUrlsOnly() {
        List<UrlInfo> results = urlInfoRepository.findAllByUserIdOrderByCreatedDatetimeDesc(testUser.getId());

        assertThat(results).hasSize(2);
        assertThat(results.get(0).getShortCode()).isEqualTo("xyz789");
        assertThat(results.get(1).getShortCode()).isEqualTo("abc123");

        // Verify the deleted URL is not included
        assertThat(results).noneMatch(dto -> dto.getShortCode().equals("del456"));
    }

    @Test
    void findAllByUserIdOrderByCreatedDatetimeDesc_WhenNoUrls_ShouldReturnEmptyList() {
        List<UrlInfo> results = urlInfoRepository.findAllByUserIdOrderByCreatedDatetimeDesc(999L);
        assertThat(results).isEmpty();
    }

    @Test
    void findAllByUserIdOrderByCreatedDatetimeDesc_ShouldReturnCorrectDtoFields() {
        // When
        List<UrlInfo> results = urlInfoRepository.findAllByUserIdOrderByCreatedDatetimeDesc(testUser.getId());

        // Then
        UrlInfo dto = results.getFirst();
        assertNotNull(dto.getId());
        assertNotNull(dto.getShortCode());
        assertEquals(UrlInfoStatus.ACTIVE, dto.getStatus());
        assertNotNull(dto.getOriginalUrl());
        assertTrue(dto.getAlias());
        assertNotNull(dto.getCreatedDatetime());
        assertNotNull(dto.getLastAccessDatetime());
    }

    @Test
    void deleteByIdAndUserId_WhenActiveUrl_ShouldSoftDelete() {
        Long urlId = testUrlInfo1.getId();
        Long userId = testUrlInfo1.getUserId();

        urlInfoRepository.deleteByIdAndUserId(urlId, userId);
        entityManager.flush();
        entityManager.clear();

        UrlInfo deleted = entityManager.find(UrlInfo.class, urlId);
        assertThat(deleted.getStatus()).isEqualTo(UrlInfoStatus.DELETED);
        assertThat(deleted.getDeletedDatetime()).isNotNull();
    }

    @Test
    void deleteByIdAndUserId_WhenWrongUserId_ShouldNotDelete() {
        Long urlId = testUrlInfo1.getId();
        Long wrongUserId = 999L;

        urlInfoRepository.deleteByIdAndUserId(urlId, wrongUserId);
        entityManager.flush();
        entityManager.clear();

        UrlInfo notDeleted = entityManager.find(UrlInfo.class, urlId);
        assertThat(notDeleted.getStatus()).isEqualTo(UrlInfoStatus.ACTIVE);
        assertThat(notDeleted.getDeletedDatetime()).isNull();
    }

    @Test
    void deleteByIdAndUserId_WhenAlreadyDeleted_ShouldNotUpdate() {
        Long urlId = deletedUrlInfo.getId();
        Long userId = deletedUrlInfo.getUserId();
        LocalDateTime originalDeletedDatetime = deletedUrlInfo.getDeletedDatetime();

        urlInfoRepository.deleteByIdAndUserId(urlId, userId);
        entityManager.flush();
        entityManager.clear();

        UrlInfo stillDeleted = entityManager.find(UrlInfo.class, urlId);
        assertThat(stillDeleted.getStatus()).isEqualTo(UrlInfoStatus.DELETED);
        assertThat(stillDeleted.getDeletedDatetime()).isEqualToIgnoringNanos(originalDeletedDatetime);
    }

    @Test
    void deleteByIdAndUserId_WhenUrlNotExists_ShouldNotThrowException() {
        Long nonExistentId = 999L;
        Long userId = 1L;

        urlInfoRepository.deleteByIdAndUserId(nonExistentId, userId);
        entityManager.flush();
    }
}
