package com.diepnn.shortenurl.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AccessTokenRequest {
    @NotBlank(message = "Username is required")
    @Schema(example = "ageha-chou", requiredMode = Schema.RequiredMode.REQUIRED)
    private String username;

    @NotBlank(message = "Password is required")
    @Schema(example = "P@ssw0rd", requiredMode = Schema.RequiredMode.REQUIRED)
    private String password;
}
