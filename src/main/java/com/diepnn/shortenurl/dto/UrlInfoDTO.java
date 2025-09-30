package com.diepnn.shortenurl.dto;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UrlInfoDTO {
    private Long id;
    private String shortUrl;
    private String status;
    private String originalUrl;
    private Boolean alias;
    private LocalDateTime createdDatetime;
    private LocalDateTime lastAccessDatetime;
}