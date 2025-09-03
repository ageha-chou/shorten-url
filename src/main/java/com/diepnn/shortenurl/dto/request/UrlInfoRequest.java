package com.diepnn.shortenurl.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class UrlInfoRequest {
    @NotBlank
    private String originalUrl;

    @Pattern(regexp = "^(?!-)([A-Za-z0-9-]{5,30})(?<!-)$", message = "Alias must be 5-30 chars, letters, digits or hyphens")
    private String alias;
}
