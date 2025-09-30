package com.diepnn.shortenurl.repository;

import com.diepnn.shortenurl.dto.UrlInfoDTO;
import com.diepnn.shortenurl.dto.cache.UrlInfoCache;
import com.diepnn.shortenurl.entity.UrlInfo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UrlInfoRepository extends JpaRepository<UrlInfo, Long> {
    Optional<UrlInfoCache> findUrlInfoCacheByShortCode(String shortCode);

    @Modifying
    @Query("UPDATE UrlInfo u SET u.lastAccessDatetime = :lastAccessDatetime WHERE u.id = :id")
    void updateLastAccessDatetimeById(Long id, LocalDateTime lastAccessDatetime);

    @Query("""
    SELECT new com.diepnn.shortenurl.dto.UrlInfoDTO(
           u.id, u.shortCode, cast(u.status as string), u.originalUrl, u.alias,
           u.createdDatetime, u.lastAccessDatetime)
           FROM UrlInfo u
           WHERE u.userId = :userId
           ORDER BY u.createdDatetime DESC
    """)
    List<UrlInfoDTO> findAllUrlInfoDtosByUserIdOrderByCreatedDatetimeDesc(Long userId);
}
