package com.diepnn.shortenurl.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.diepnn.shortenurl.entity.UrlInfo;

@Repository
public interface UrlInfoRepository extends JpaRepository<UrlInfo, Long> {
    Optional<UrlInfo> findByShortUrl(String shortUrl);
}
