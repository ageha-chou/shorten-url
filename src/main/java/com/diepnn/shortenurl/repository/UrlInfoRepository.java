package com.diepnn.shortenurl.repository;

import com.diepnn.shortenurl.dto.cache.UrlInfoCache;
import com.diepnn.shortenurl.entity.UrlInfo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface UrlInfoRepository extends JpaRepository<UrlInfo, Long> {
    @Query("""
           SELECT u.id, u.originalUrl
           FROM UrlInfo u
           WHERE u.shortCode = :shortCode AND u.status = com.diepnn.shortenurl.common.enums.UrlInfoStatus.ACTIVE
           """)
    UrlInfoCache findUrlInfoCacheByShortCode(String shortCode);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE UrlInfo u SET u.lastAccessDatetime = :lastAccessDatetime WHERE u.id = :id")
    void updateLastAccessDatetimeById(Long id, LocalDateTime lastAccessDatetime);

    @Query("""
           SELECT u
           FROM UrlInfo u
           WHERE u.userId = :userId AND u.status = com.diepnn.shortenurl.common.enums.UrlInfoStatus.ACTIVE
           ORDER BY u.createdDatetime DESC
        """)
    List<UrlInfo> findAllByUserIdOrderByCreatedDatetimeDesc(Long userId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(
           """
           UPDATE UrlInfo u SET u.deletedDatetime = CURRENT_TIMESTAMP,
                  u.status = com.diepnn.shortenurl.common.enums.UrlInfoStatus.DELETED
           WHERE u.id = :id AND u.userId = :userId AND u.status = com.diepnn.shortenurl.common.enums.UrlInfoStatus.ACTIVE
           """)
    void deleteByIdAndUserId(Long id, Long userId);
}
