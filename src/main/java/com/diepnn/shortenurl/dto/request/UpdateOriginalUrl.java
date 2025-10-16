package com.diepnn.shortenurl.dto.request;

import com.diepnn.shortenurl.common.annotation.validation.OriginalUrl;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UpdateOriginalUrl {
    @OriginalUrl
    @Schema(example = "https://github.com/ageha-chou", requiredMode = Schema.RequiredMode.REQUIRED)
    private String originalUrl;
}
