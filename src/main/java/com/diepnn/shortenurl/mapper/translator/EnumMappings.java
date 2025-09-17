package com.diepnn.shortenurl.mapper.translator;

import com.diepnn.shortenurl.common.annotation.EnumTranslator;
import com.diepnn.shortenurl.common.annotation.FromEnum;
import com.diepnn.shortenurl.common.annotation.ToEnum;
import com.diepnn.shortenurl.common.enums.PersistableEnum;
import com.diepnn.shortenurl.utils.EnumUtils;
import org.mapstruct.TargetType;
import org.springframework.stereotype.Component;

/**
 * Enum translator for MapStruct mapping
 */
@Component
@EnumTranslator
public class EnumMappings {
    /**
     * Convert string to enum.
     *
     * @param value string value
     * @param enumType enum type
     * @return enum value. Return null if the string is null or not found in the enum
     */
    @ToEnum
    public <E extends Enum<E> & PersistableEnum> E toEnum(String value, @TargetType Class<E> enumType) {
        if (value == null) {
            return null;
        }

        return EnumUtils.fromValue(enumType, value);
    }

    /**
     * Convert enum to string.
     *
     * @param enumValue enum value
     * @return string value. Return null if the enum is null
     */
    @FromEnum
    public <E extends Enum<E> & PersistableEnum> String fromEnum(E enumValue) {
        return (enumValue == null) ? null : enumValue.getValue();
    }
}
