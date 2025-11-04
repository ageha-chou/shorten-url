package com.diepnn.shortenurl.repository;

import com.diepnn.shortenurl.dto.cache.UrlInfoCache;
import com.diepnn.shortenurl.dto.filter.UrlInfoFilter;
import com.diepnn.shortenurl.entity.UrlInfo;
import com.diepnn.shortenurl.specification.UrlInfoSpecs;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface UrlInfoRepository extends JpaRepository<UrlInfo, Long>, JpaSpecificationExecutor<UrlInfo> {
    /**
     * Find the UrlInfo by shortCode and status is ACTIVE. Only extract id and originalUrl.
     *
     * @param shortCode shortCode
     * @return UrlInfoCache
     */
    @Query("""
           SELECT u.id, u.originalUrl
           FROM UrlInfo u
           WHERE u.shortCode = :shortCode AND u.status = com.diepnn.shortenurl.common.enums.UrlInfoStatus.ACTIVE
           """)
    UrlInfoCache findUrlInfoCacheByShortCode(String shortCode);

    /**
     * Find all url info by filter.
     *
     * @param spec
     * @param pageable
     * @return url info list
     */
    Page<UrlInfo> findAll(Specification<UrlInfo> spec, Pageable pageable);

    /**
     * Find all url info by filter (published for user calls directly using filter)
     *
     * @param filter
     * @param pageable
     * @return
     */
    default Page<UrlInfo> findAll(UrlInfoFilter filter, Pageable pageable) {
        return findAll(UrlInfoSpecs.from(filter), pageable);
    }

    /**
     * Find all url info by user id and status is ACTIVE.
     *
     * @param userId user id
     * @return url info list
     */
    @Query("""
           SELECT u
           FROM UrlInfo u
           WHERE u.userId = :userId AND u.status = com.diepnn.shortenurl.common.enums.UrlInfoStatus.ACTIVE
           ORDER BY u.createdDatetime DESC
           """)
    List<UrlInfo> findAllByUserIdOrderByCreatedDatetimeDesc(Long userId);

    /**
     * Update last access datetime by id.
     *
     * @param id url info id
     * @param lastAccessDatetime last access datetime
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE UrlInfo u SET u.lastAccessDatetime = :lastAccessDatetime WHERE u.id = :id")
    void updateLastAccessDatetimeById(Long id, LocalDateTime lastAccessDatetime);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
           UPDATE UrlInfo u
           SET u.status = com.diepnn.shortenurl.common.enums.UrlInfoStatus.DEACTIVATE,
               u.deactivatedBy = :userId, u.deactivatedReason = :reason,
               u.deactivatedDatetime = CURRENT_TIMESTAMP
           WHERE u.id = :urlId AND u.status = com.diepnn.shortenurl.common.enums.UrlInfoStatus.ACTIVE
           """)
    void deactivateUrlInfo(Long urlId, String reason, Long userId);

    /**
     * Soft delete url info by id and user id.
     *
     * @param id url info id
     * @param userId user id
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(
           """
           UPDATE UrlInfo u SET u.deletedDatetime = CURRENT_TIMESTAMP,
                  u.status = com.diepnn.shortenurl.common.enums.UrlInfoStatus.DELETED
           WHERE u.id = :id AND u.userId = :userId AND u.status = com.diepnn.shortenurl.common.enums.UrlInfoStatus.ACTIVE
           """)
    void deleteByIdAndUserId(Long id, Long userId);

}
