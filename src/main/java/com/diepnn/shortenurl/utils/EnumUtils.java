package com.diepnn.shortenurl.utils;

import com.diepnn.shortenurl.common.enums.PersistableEnum;
import org.apache.commons.lang3.StringUtils;

public class EnumUtils {
    public static <E extends Enum<E> & PersistableEnum> E fromValue(Class<E> enumClass, String value) {
        if (StringUtils.isBlank(value)) {
            return null;
        }

        value = value.toLowerCase();
        for (E enumConstant : enumClass.getEnumConstants()) {
            if (enumConstant.getValue().toLowerCase().equals(value)) {
                return enumConstant;
            }
        }

        throw new IllegalArgumentException("Unknown value: " + value + " for enum " + enumClass.getSimpleName());
    }
}
