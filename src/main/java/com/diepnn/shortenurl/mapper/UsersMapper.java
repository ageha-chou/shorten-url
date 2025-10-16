package com.diepnn.shortenurl.mapper;

import com.diepnn.shortenurl.dto.UserDTO;
import com.diepnn.shortenurl.dto.request.UserUpdateRequest;
import com.diepnn.shortenurl.dto.request.UsernamePasswordSignupRequest;
import com.diepnn.shortenurl.entity.Users;
import com.diepnn.shortenurl.utils.PasswordEncoderMapperHelper;
import lombok.RequiredArgsConstructor;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;

@Mapper(uses = {AuthProviderMapper.class, PasswordEncoderMapperHelper.class})
@RequiredArgsConstructor
public abstract class UsersMapper implements BaseMapper<Users, UserDTO> {
    public abstract UserDTO toDto(Users users);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "status", constant = "ACTIVE")
    @Mapping(target = "createdDatetime", expression = "java(java.time.LocalDateTime.now())")
    @Mapping(target = "authProviders", constant = "LOCAL")
    @Mapping(target = "username", expression = "java(org.apache.commons.lang3.StringUtils.lowerCase(userRequest.getUsername()))")
    @Mapping(target = "email", expression = "java(org.apache.commons.lang3.StringUtils.lowerCase(userRequest.getEmail()))")
    @Mapping(target = "password", qualifiedByName = "encodePassword")
    @Mapping(target = "avatar", ignore = true)
    @Mapping(target = "updatedDatetime", ignore = true)
    public abstract Users toEntity(UsernamePasswordSignupRequest userRequest);

    @BeanMapping(unmappedTargetPolicy = ReportingPolicy.IGNORE)
    @Mapping(target = "updatedDatetime", expression = "java(java.time.LocalDateTime.now())")
    public abstract void updateEntity(@MappingTarget Users user, UserUpdateRequest userRequest);
}
