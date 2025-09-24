package com.diepnn.shortenurl.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AccessTokenRequest {
    @NotBlank(message = "Username is required")
    private String username;

    @NotBlank(message = "Password is required")
    private String password;
}
