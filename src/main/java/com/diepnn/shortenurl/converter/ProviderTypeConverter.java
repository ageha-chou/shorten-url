package com.diepnn.shortenurl.converter;

import com.diepnn.shortenurl.common.enums.ProviderType;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class ProviderTypeConverter extends EnumAttributeConverter<ProviderType> {
    public ProviderTypeConverter() {
        super(ProviderType.class);
    }
}
