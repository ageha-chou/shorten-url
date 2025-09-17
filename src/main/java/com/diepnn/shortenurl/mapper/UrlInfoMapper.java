package com.diepnn.shortenurl.mapper;

import com.diepnn.shortenurl.common.annotation.EnumTranslator;
import com.diepnn.shortenurl.common.annotation.FromEnum;
import com.diepnn.shortenurl.common.annotation.FromShortUrl;
import com.diepnn.shortenurl.common.annotation.ShortUrlTranslator;
import com.diepnn.shortenurl.common.annotation.ToEnum;
import com.diepnn.shortenurl.common.annotation.ToShortUrl;
import com.diepnn.shortenurl.dto.UrlInfoDTO;
import com.diepnn.shortenurl.entity.UrlInfo;
import com.diepnn.shortenurl.mapper.translator.EnumMappings;
import com.diepnn.shortenurl.mapper.translator.ShortUrlMappings;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Mapper for converting between {@link UrlInfo} and {@link UrlInfoDTO}
 */
@Mapper(uses = {EnumMappings.class, ShortUrlMappings.class})
public abstract class UrlInfoMapper implements BaseMapper<UrlInfo, UrlInfoDTO>{
    @Mapping(target = "status", qualifiedBy = {EnumTranslator.class, FromEnum.class})
    @Mapping(target = "shortUrl", source = "shortCode", qualifiedBy = {ShortUrlTranslator.class, ToShortUrl.class})
    @Override
    public abstract UrlInfoDTO toDto(UrlInfo s);

    @Mapping(target = "status", qualifiedBy = {EnumTranslator.class, ToEnum.class})
    @Mapping(target = "shortCode", source = "shortUrl", qualifiedBy = {ShortUrlTranslator.class, FromShortUrl.class})
    @Override
    public abstract UrlInfo toEntity(UrlInfoDTO s);
}
