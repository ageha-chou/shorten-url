package com.diepnn.shortenurl.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class UsernamePasswordSignupRequest {
    @Pattern(regexp = "^[a-zA-Z0-9_-]{5,30}$", message = "Username is required and must be 5-30 chars, letters, digits or underscores")
    @Schema(example = "ageha-chou", requiredMode = Schema.RequiredMode.REQUIRED)
    private String username;

    @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,}$",
             message = "Password is required and must be at least 8 chars, contain at least one uppercase letter, one lowercase letter, one digit and one special character")
    @Schema(example = "P@ssw0rd", requiredMode = Schema.RequiredMode.REQUIRED)
    private String password;

    @NotNull(message = "First name is required")
    @Pattern(regexp = "^[\\p{L}\\p{M}]{1,30}$", message = "First name is required and must only contain letters")
    @Schema(example = "First name", requiredMode = Schema.RequiredMode.REQUIRED)
    private String firstName;

    @NotNull(message = "Last name is required")
    @Schema(example = "Last name", requiredMode = Schema.RequiredMode.REQUIRED)
    @Pattern(regexp = "^[\\p{L}\\p{M}]{1,30}$", message = "Last name is required and must only contain letters")
    private String lastName;

    @Email(message = "Invalid email")
    @Schema(example = "example@gmail.com", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private String email;
}
