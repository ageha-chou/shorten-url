package com.diepnn.shortenurl.converter;

import com.diepnn.shortenurl.common.enums.PersistableEnum;
import com.diepnn.shortenurl.utils.EnumUtils;
import jakarta.persistence.AttributeConverter;
import lombok.RequiredArgsConstructor;

/**
 * Base class for custom JPA enum converters.
 * @param <E> Enum type. Enum must implement {@link PersistableEnum}
 */
@RequiredArgsConstructor
public abstract class EnumAttributeConverter <E extends Enum<E> & PersistableEnum> implements AttributeConverter<E, String> {
    private final Class<E> enumClass;

    @Override
    public String convertToDatabaseColumn(E attribute) {
        return attribute == null ? null : attribute.getValue();
    }

    @Override
    public E convertToEntityAttribute(String dbData) {
        return EnumUtils.fromValue(enumClass, dbData);
    }
}
