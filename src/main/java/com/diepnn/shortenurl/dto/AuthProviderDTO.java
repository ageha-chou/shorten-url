package com.diepnn.shortenurl.dto;

import com.diepnn.shortenurl.common.enums.ProviderType;

import java.time.LocalDateTime;

public record AuthProviderDTO (ProviderType providerType, LocalDateTime createdDatetime, LocalDateTime lastAccessDatetime) {
}
