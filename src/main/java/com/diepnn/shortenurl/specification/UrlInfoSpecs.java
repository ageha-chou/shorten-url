package com.diepnn.shortenurl.specification;

import com.diepnn.shortenurl.common.enums.UrlInfoStatus;
import com.diepnn.shortenurl.dto.filter.UrlInfoFilter;
import com.diepnn.shortenurl.entity.UrlInfo;
import org.springframework.data.jpa.domain.Specification;


import static com.diepnn.shortenurl.utils.SpecUtils.between;
import static com.diepnn.shortenurl.utils.SpecUtils.containsIgnoreCase;
import static com.diepnn.shortenurl.utils.SpecUtils.equalsTo;
import static com.diepnn.shortenurl.utils.SpecUtils.equalsIgnoreCase;
import static com.diepnn.shortenurl.utils.SpecUtils.inValues;

public class UrlInfoSpecs {
    private UrlInfoSpecs() {
    }

    public static Specification<UrlInfo> from(UrlInfoFilter f) {
        return Specification.<UrlInfo>unrestricted()
                            .and(inValues("status", f.statuses()))
                            .and(equalsTo("alias", f.alias()))
                            .and(equalsIgnoreCase("shortCode", f.shortCode()))
                            .and(containsIgnoreCase("originalUrl", f.originalUrlPattern()))
                            .and(inValues("userId", f.userIds()))
                            .and(between("createdDatetime", f.createdFrom(), f.createdTo()));
    }
}
