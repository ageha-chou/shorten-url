package com.diepnn.shortenurl.mapper;

import com.diepnn.shortenurl.dto.UrlInfoDTO;
import com.diepnn.shortenurl.entity.UrlInfo;
import org.springframework.stereotype.Component;

@Component
public class UrlInfoMapper implements BaseMapper<UrlInfo, UrlInfoDTO> {
    @Override
    public UrlInfoDTO toDTO(UrlInfo entity) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public UrlInfo toEntity(UrlInfoDTO dto) {
        // TODO Auto-generated method stub
        return null;
    }
}
