package com.diepnn.shortenurl.dto;

import java.time.LocalDateTime;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UrlInfoDTO {
    private Long id;
    private String shortUrl;
    private String status;
    private String originalUrl;
    private Boolean alias;
    private LocalDateTime createdDatetime;
    private LocalDateTime lastAccessDatetime;
}