package com.diepnn.shortenurl.service.cache;

import com.diepnn.shortenurl.dto.UrlInfoDTO;
import com.diepnn.shortenurl.dto.cache.UrlInfoCache;
import com.diepnn.shortenurl.entity.UrlInfo;
import com.diepnn.shortenurl.mapper.UrlInfoMapper;
import com.diepnn.shortenurl.repository.UrlInfoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class UrlInfoCacheServiceTests {
    @Mock
    private UrlInfoRepository urlInfoRepository;

    @Mock
    private UrlInfoMapper urlInfoMapper;

    @InjectMocks
    private UrlInfoCacheService urlInfoCacheService;

    private UrlInfoCache mockCache;

    @BeforeEach
    void setUp() {
        mockCache = new UrlInfoCache(1L, "https://example.com");
    }

    @Test
    void findByShortCodeCache_shouldReturnCache_whenFound() {
        when(urlInfoRepository.findUrlInfoCacheByShortCode("abc123")).thenReturn(mockCache);

        UrlInfoCache result = urlInfoCacheService.findByShortCodeCache("abc123");

        assertNotNull(result);
        assertEquals("https://example.com", result.originalUrl());
        verify(urlInfoRepository).findUrlInfoCacheByShortCode("abc123");
    }

    @Test
    void findByShortCodeCache_shouldReturnNull_whenNotExists() {
        // Arrange
        when(urlInfoRepository.findUrlInfoCacheByShortCode("notfound")).thenReturn(null);

        // Act & Assert
        UrlInfoCache urlInfoCache = urlInfoCacheService.findByShortCodeCache("notfound");
        assertNull(urlInfoCache);
        verify(urlInfoRepository).findUrlInfoCacheByShortCode("notfound");
    }

    @Test
    void findAllByUserId_shouldReturnList_whenRepositoryReturnsData() {
        // Arrange
        Long userId = 100L;
        UrlInfo urlInfo = UrlInfo.builder()
                                 .id(1L)
                                 .userId(userId)
                                 .shortCode("abc123")
                                 .originalUrl("https://example.com")
                                 .build();

        UrlInfoDTO dto = UrlInfoDTO.builder()
                                   .id(1L)
                                   .shortUrl("http://abc123")
                                   .build();

        when(urlInfoRepository.findAllByUserIdOrderByCreatedDatetimeDesc(userId)).thenReturn(List.of(urlInfo));
        when(urlInfoMapper.toDtos(List.of(urlInfo))).thenReturn(List.of(dto));

        // Act
        List<UrlInfoDTO> result = urlInfoCacheService.findAllByUserId(userId);

        // Assert
        assertEquals(1, result.size());
        assertEquals("http://abc123", result.getFirst().getShortUrl());
        verify(urlInfoRepository).findAllByUserIdOrderByCreatedDatetimeDesc(userId);
    }

    @Test
    void evictUserUrlsCache_shouldNotThrowException() {
        // Act & Assert (no exception should occur)
        assertDoesNotThrow(() -> urlInfoCacheService.evictUserUrlsCache(100L));
    }
}
