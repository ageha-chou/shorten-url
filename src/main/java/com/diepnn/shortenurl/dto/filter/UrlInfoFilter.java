package com.diepnn.shortenurl.dto.filter;

import com.diepnn.shortenurl.common.enums.UrlInfoStatus;

import java.time.Instant;
import java.util.Set;

public record UrlInfoFilter(
        Set<UrlInfoStatus> statuses,
        Boolean alias,
        String shortCode,
        String originalUrlPattern,
        Set<Long> userIds,
        Instant createdFrom,
        Instant createdTo
) {}
