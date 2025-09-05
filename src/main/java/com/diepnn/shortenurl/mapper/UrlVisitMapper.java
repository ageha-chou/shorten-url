package com.diepnn.shortenurl.mapper;

import com.diepnn.shortenurl.dto.UserInfo;
import com.diepnn.shortenurl.entity.UrlVisit;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper
public abstract class UrlVisitMapper {
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "shortenUrl", ignore = true)
    public abstract UrlVisit toEntity(UserInfo userInfo);
}
