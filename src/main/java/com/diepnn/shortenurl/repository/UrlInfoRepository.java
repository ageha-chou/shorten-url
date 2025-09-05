package com.diepnn.shortenurl.repository;

import java.time.LocalDateTime;
import java.util.Optional;

import jakarta.persistence.QueryHint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.stereotype.Repository;

import com.diepnn.shortenurl.entity.UrlInfo;

@Repository
public interface UrlInfoRepository extends JpaRepository<UrlInfo, Long> {
    @QueryHints({ @QueryHint(name = "org.hibernate.readOnly", value ="true") })
    Optional<UrlInfo> findByShortCode(String shortCode);

    @Modifying
    @Query("UPDATE UrlInfo u SET u.lastAccessDatetime = :lastAccessDatetime WHERE u.id = :id")
    void updateLastAccessDatetimeById(Long id, LocalDateTime lastAccessDatetime);
}
