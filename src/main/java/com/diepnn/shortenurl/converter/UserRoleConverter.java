package com.diepnn.shortenurl.converter;

import com.diepnn.shortenurl.common.enums.UserRole;
import jakarta.persistence.Converter;

/**
 * JPA converter for {@link UserRole} enum.
 */
@Converter(autoApply = true)
public class UserRoleConverter extends EnumAttributeConverter<UserRole> {
    public UserRoleConverter() {
        super(UserRole.class);
    }
}
