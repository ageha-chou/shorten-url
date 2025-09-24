package com.diepnn.shortenurl.dto;

import java.time.LocalDateTime;
import java.util.List;

public record UserDTO (String username, String email, String firstName, String lastName,
                       LocalDateTime createdDatetime, List<AuthProviderDTO> authProviders) {
}
