package com.diepnn.shortenurl.dto.request;

import com.diepnn.shortenurl.common.annotation.validation.OriginalUrl;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UpdateOriginalUrl {
    @OriginalUrl
    private String originalUrl;
}
