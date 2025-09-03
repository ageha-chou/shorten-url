package com.diepnn.shortenurl.common.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public enum UrlInfoStatus {
    ACTIVE("A"),
    EXPIRED("E");

    private final String value;
}
