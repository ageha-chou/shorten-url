package com.diepnn.shortenurl.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class UsernamePasswordSignupRequest {
    @Pattern(regexp = "^[a-zA-Z0-9_-]{5,30}$", message = "Username is required and must be 5-30 chars, letters, digits or underscores")
    private String username;

    @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,}$",
             message = "Password is required and must be at least 8 chars, contain at least one uppercase letter, one lowercase letter, one digit and one special character")
    private String password;

    @Pattern(regexp = "^[\\p{L}\\p{M}]{1,30}$", message = "First name is required and must only contain letters")
    private String firstName;

    @Pattern(regexp = "^[\\p{L}\\p{M}]{1,30}$", message = "Last name is required and must only contain letters")
    private String lastName;

    @Email(message = "Invalid email")
    private String email;
}
