package com.diepnn.shortenurl.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class UserUpdateRequest {
    @Pattern(regexp = "^[\\p{L}\\p{M}]{1,30}$", message = "First name is required and must only contain letters")
    @Schema(example = "Firstname", requiredMode = Schema.RequiredMode.REQUIRED)
    private String firstName;

    @Pattern(regexp = "^[\\p{L}\\p{M}]{1,30}$", message = "Last name is required and must only contain letters")
    @Schema(example = "Lastname", requiredMode = Schema.RequiredMode.REQUIRED)
    private String lastName;
}
