package com.diepnn.shortenurl.controller;

import com.diepnn.shortenurl.dto.request.AccessTokenRequest;
import com.diepnn.shortenurl.dto.response.AccessTokenResponse;
import com.diepnn.shortenurl.dto.response.BaseResponseWrapper;
import com.diepnn.shortenurl.dto.response.InvalidResponseWrapper;
import com.diepnn.shortenurl.security.JwtService;
import com.diepnn.shortenurl.utils.ResponseWrapperBuilder;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * The controller for authenticating users.
 */
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Authentication API")
public class AuthController {
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    /**
     * Login with username and password.
     *
     * @param userRequest the username and password
     * @return the generated access token
     */
    @Operation(summary = "Login with username and password")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Created successfully", useReturnTypeSchema = true),
            @ApiResponse(responseCode = "400", description = "Invalid request",
                         content = @Content(schema = @Schema(implementation = InvalidResponseWrapper.class))
            )
    })
    @PostMapping("/access-token")
    @ResponseStatus(HttpStatus.CREATED)
    public BaseResponseWrapper<AccessTokenResponse> loginWithUsernameAndPassword(@Valid @RequestBody AccessTokenRequest userRequest) {
        UsernamePasswordAuthenticationToken authToken =
                new UsernamePasswordAuthenticationToken(userRequest.getUsername(), userRequest.getPassword());

        Authentication authenticate = authenticationManager.authenticate(authToken);

        AccessTokenResponse response = new AccessTokenResponse(jwtService.generateToken(authenticate));
        return ResponseWrapperBuilder.withData(HttpStatus.CREATED, "Token generated successfully", response);
    }
}
