package com.diepnn.shortenurl.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class UserUpdateRequest {
    @NotBlank(message = "First name is required")
    @Pattern(regexp = "^[\\p{L}\\p{M}]{1,30}$", message = "First name must only contain letters")
    private String firstName;

    @NotBlank(message = "Last name is required")
    @Pattern(regexp = "^[\\p{L}\\p{M}]{1,30}$", message = "Last name must only contain letters")
    private String lastName;
}
