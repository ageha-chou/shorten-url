package com.diepnn.shortenurl.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class DeactivateUrlInfo {
    @NotNull(message = "Deactivated reason is required")
    @Size(min = 1, max = 200, message = "Deactivated reason must be between 1 and 200 characters")
    private String deactivatedReason;
}
