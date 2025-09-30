package com.diepnn.shortenurl.mapper;

import com.diepnn.shortenurl.common.enums.ProviderType;
import com.diepnn.shortenurl.dto.AuthProviderDTO;
import com.diepnn.shortenurl.entity.AuthProvider;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

/**
 * Mapper for converting between {@link AuthProvider} and {@link AuthProviderDTO}
 */
@Mapper
public abstract class AuthProviderMapper implements BaseMapper<AuthProvider, AuthProviderDTO> {
    @Mapping(target = "providerType", source = "providerType")
    @Mapping(target = "createdDatetime", expression = "java(java.time.LocalDateTime.now())")
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "lastAccessDatetime", ignore = true)
    @Mapping(target = "providerUserId", ignore = true)
    public abstract AuthProvider toEntity(ProviderType providerType);

    public abstract AuthProviderDTO toDto(AuthProvider authProvider);

    public List<AuthProvider> mapToAuthProviders(ProviderType providerType) {
        return List.of(toEntity(providerType != null ? providerType : ProviderType.LOCAL));
    }
}
