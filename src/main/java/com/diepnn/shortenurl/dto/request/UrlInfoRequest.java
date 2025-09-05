package com.diepnn.shortenurl.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UrlInfoRequest {
    @NotBlank(message = "Original URL is required")
    @Pattern(regexp = "^(?:(https?)://)?(?:[a-zA-Z0-9.-]+(?::[0-9]+)?|\\d{1,3}(?:\\.\\d{1,3}){3}(?::\\d+)?)(?:/.*)?$",
             flags = {Pattern.Flag.CASE_INSENSITIVE},
             message = "Invalid URL")
    private String originalUrl;

    @Pattern(regexp = "^(?!-)([A-Za-z0-9-]{5,30})(?<!-)$", message = "Alias must be 5-30 chars, letters, digits or hyphens")
    private String alias;
}
