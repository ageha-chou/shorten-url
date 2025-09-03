package com.diepnn.shortenurl.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.diepnn.shortenurl.entity.UrlVisit;

@Repository
public interface UrlVisitRepository extends JpaRepository<UrlVisit, Long> {

}
