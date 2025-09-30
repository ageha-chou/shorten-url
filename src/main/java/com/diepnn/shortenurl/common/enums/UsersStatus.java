package com.diepnn.shortenurl.common.enums;

import com.diepnn.shortenurl.utils.EnumUtils;
import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum UsersStatus implements PersistableEnum {
    ACTIVE("A"),
    BLOCKED("BL"),
    BANNED("BA");

    private final String value;

    @Override
    public String getValue() {
        return value;
    }

    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static UsersStatus fromValue(String value) {
        return EnumUtils.fromValue(UsersStatus.class, value);
    }
}
