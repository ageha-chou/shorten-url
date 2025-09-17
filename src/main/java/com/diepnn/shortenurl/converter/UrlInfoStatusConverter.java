package com.diepnn.shortenurl.converter;

import com.diepnn.shortenurl.common.enums.UrlInfoStatus;
import jakarta.persistence.Converter;

/**
 * JPA converter for {@link UrlInfoStatus} enum.
 */
@Converter(autoApply = true)
public class UrlInfoStatusConverter extends EnumAttributeConverter<UrlInfoStatus>{
    public UrlInfoStatusConverter() {
        super(UrlInfoStatus.class);
    }
}
