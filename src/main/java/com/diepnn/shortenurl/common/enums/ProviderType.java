package com.diepnn.shortenurl.common.enums;

import com.diepnn.shortenurl.utils.EnumUtils;
import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum ProviderType implements PersistableEnum {
    LOCAL("LOCAL"),
    GOOGLE("GOOGLE"),
    FACEBOOK("FACEBOOK");

    private final String value;

    @Override
    public String getValue() {
        return value;
    }

    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static ProviderType fromValue(String value) {
        return EnumUtils.fromValue(ProviderType.class, value);
    }
}
