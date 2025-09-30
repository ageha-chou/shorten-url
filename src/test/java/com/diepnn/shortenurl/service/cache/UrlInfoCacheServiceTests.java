package com.diepnn.shortenurl.service.cache;

import com.diepnn.shortenurl.dto.UrlInfoDTO;
import com.diepnn.shortenurl.dto.cache.UrlInfoCache;
import com.diepnn.shortenurl.exception.NotFoundException;
import com.diepnn.shortenurl.repository.UrlInfoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class UrlInfoCacheServiceTests {
    @Mock
    private UrlInfoRepository urlInfoRepository;

    @InjectMocks
    private UrlInfoCacheService urlInfoCacheService;

    private UrlInfoCache mockCache;

    @BeforeEach
    void setUp() {
        mockCache = new UrlInfoCache(1L, "https://example.com");
    }

    @Test
    void findByShortCodeCache_shouldReturnCache_whenFound() {
        when(urlInfoRepository.findUrlInfoCacheByShortCode("abc123")).thenReturn(Optional.of(mockCache));

        UrlInfoCache result = urlInfoCacheService.findByShortCodeCache("abc123");

        assertNotNull(result);
        assertEquals("https://example.com", result.originalUrl());
        verify(urlInfoRepository).findUrlInfoCacheByShortCode("abc123");
    }

    @Test
    void findByShortCodeCache_shouldThrowNotFound_whenNotExists() {
        // Arrange
        when(urlInfoRepository.findUrlInfoCacheByShortCode("notfound")).thenReturn(Optional.empty());

        // Act & Assert
        NotFoundException ex = assertThrows(NotFoundException.class, () -> urlInfoCacheService.findByShortCodeCache("notfound"));
        assertTrue(ex.getMessage().contains("Not found URL: notfound"));
        verify(urlInfoRepository).findUrlInfoCacheByShortCode("notfound");
    }

    @Test
    void findAllByUserId_shouldReturnList_whenRepositoryReturnsData() {
        // Arrange
        Long userId = 100L;
        UrlInfoDTO dto = new UrlInfoDTO();
        dto.setShortUrl("abc123");
        dto.setOriginalUrl("https://example.com");

        when(urlInfoRepository.findAllUrlInfoDtosByUserIdOrderByCreatedDatetimeDesc(userId)).thenReturn(List.of(dto));

        // Act
        List<UrlInfoDTO> result = urlInfoCacheService.findAllByUserId(userId);

        // Assert
        assertEquals(1, result.size());
        assertEquals("abc123", result.getFirst().getShortUrl());
        verify(urlInfoRepository).findAllUrlInfoDtosByUserIdOrderByCreatedDatetimeDesc(userId);
    }

    @Test
    void evictUserUrlsCache_shouldNotThrowException() {
        // Act & Assert (no exception should occur)
        assertDoesNotThrow(() -> urlInfoCacheService.evictUserUrlsCache(100L));
    }
}
