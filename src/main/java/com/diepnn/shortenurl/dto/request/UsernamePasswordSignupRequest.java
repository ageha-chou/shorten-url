package com.diepnn.shortenurl.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class UsernamePasswordSignupRequest {
    @NotBlank(message = "Username is required")
    @Pattern(regexp = "^[a-zA-Z0-9_-]{5,30}$", message = "Username must be 5-30 chars, letters, digits or underscores")
    private String username;

    @NotBlank(message = "Password is required")
    @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,}$",
             message = "Password must be at least 8 chars, contain at least one uppercase letter, one lowercase letter, one digit and one special character")
    private String password;

    @NotBlank(message = "First name is required")
    @Pattern(regexp = "^[\\p{L}\\p{M}]{1,30}$", message = "First name must only contain letters")
    private String firstName;

    @NotBlank(message = "Last name is required")
    @Pattern(regexp = "^[\\p{L}\\p{M}]{1,30}$", message = "Last name must only contain letters")

    private String lastName;

    @Email(message = "Invalid email")
    private String email;
}
