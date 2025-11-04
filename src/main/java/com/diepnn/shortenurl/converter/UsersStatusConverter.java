package com.diepnn.shortenurl.converter;

import com.diepnn.shortenurl.common.enums.UsersStatus;
import jakarta.persistence.Converter;

/**
 * JPA converter for {@link UsersStatus} enum.
 */
@Converter(autoApply = true)
public class UsersStatusConverter extends EnumAttributeConverter<UsersStatus> {
    public UsersStatusConverter() {
        super(UsersStatus.class);
    }
}
