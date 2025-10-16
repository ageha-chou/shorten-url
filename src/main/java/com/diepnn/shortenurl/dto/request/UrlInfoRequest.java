package com.diepnn.shortenurl.dto.request;

import com.diepnn.shortenurl.common.annotation.validation.OriginalUrl;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UrlInfoRequest {
    @OriginalUrl
    @Schema(example = "https://github.com/ageha-chou", requiredMode = Schema.RequiredMode.REQUIRED)
    private String originalUrl;

    @Pattern(regexp = "^(?!-)([A-Za-z0-9-]{5,30})(?<!-)$", message = "Alias must be 5-30 chars, letters, digits or hyphens")
    @Schema(example = "github", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private String alias;
}
