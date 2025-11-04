package com.diepnn.shortenurl.common.enums;

import com.diepnn.shortenurl.utils.EnumUtils;
import com.fasterxml.jackson.annotation.JsonCreator;

public enum UserRole implements PersistableEnum {
    ADMIN,
    USER;

    @Override
    public String getValue() {
        return this.name();
    }

    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static UserRole fromValue(String value) {
        return EnumUtils.fromValue(UserRole.class, value);
    }
}
