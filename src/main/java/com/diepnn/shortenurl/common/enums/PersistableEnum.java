package com.diepnn.shortenurl.common.enums;

import com.diepnn.shortenurl.mapper.translator.EnumMappings;

/**
 * <p>The common interface the enum to have the common properties {@code value}</p>
 * <p>{@code value} will be used in {@link EnumMappings} to convert
 * enum to String</p>
 */
public interface PersistableEnum {
    String getValue();
}
