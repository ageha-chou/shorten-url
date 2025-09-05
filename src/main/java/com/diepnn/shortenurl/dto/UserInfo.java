package com.diepnn.shortenurl.dto;

import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record UserInfo(String ipAddress, String userAgent, LocalDateTime visitedDatetime, String country) {
}
