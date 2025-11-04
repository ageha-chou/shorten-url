package com.diepnn.shortenurl.common.enums;

import com.diepnn.shortenurl.utils.EnumUtils;
import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum UrlInfoStatus implements PersistableEnum {
    ACTIVE("A"),
    EXPIRED("E"),
    DELETED("D"),
    DEACTIVATE("DE");

    private final String value;

    @Override
    public String getValue() {
        return this.value;
    }

    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static UrlInfoStatus fromValue(String value) {
        return EnumUtils.fromValue(UrlInfoStatus.class, value);
    }
}
