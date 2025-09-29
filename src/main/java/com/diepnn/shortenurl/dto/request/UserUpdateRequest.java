package com.diepnn.shortenurl.dto.request;

import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class UserUpdateRequest {
    @Pattern(regexp = "^[\\p{L}\\p{M}]{1,30}$", message = "First name is required and must only contain letters")
    private String firstName;

    @Pattern(regexp = "^[\\p{L}\\p{M}]{1,30}$", message = "Last name is required and must only contain letters")
    private String lastName;
}
