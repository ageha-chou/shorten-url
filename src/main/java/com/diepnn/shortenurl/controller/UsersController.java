package com.diepnn.shortenurl.controller;

import com.diepnn.shortenurl.dto.UserDTO;
import com.diepnn.shortenurl.dto.request.UsernamePasswordSignupRequest;
import com.diepnn.shortenurl.dto.request.UserUpdateRequest;
import com.diepnn.shortenurl.dto.response.BaseResponseWrapper;
import com.diepnn.shortenurl.dto.response.ErrorResponseWrapper;
import com.diepnn.shortenurl.dto.response.InvalidResponseWrapper;
import com.diepnn.shortenurl.security.CustomUserDetails;
import com.diepnn.shortenurl.service.UsersService;
import com.diepnn.shortenurl.utils.ResponseWrapperBuilder;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * The controller for managing users.
 */
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Tag(name = "User", description = "User API")
public class UsersController {
    private final UsersService usersService;

    /**
     * Creates a new user by username and password.
     *
     * @param userRequest contains the username and password and their additional information.
     * @return the created user.
     * @throws IllegalArgumentException if the request is invalid.
     */
    @Operation(summary = "Create a new user by username and password")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Created successfully", useReturnTypeSchema = true),
            @ApiResponse(responseCode = "400", description = "Invalid request",
                         content = @Content(schema = @Schema(implementation = InvalidResponseWrapper.class))),
            @ApiResponse(responseCode = "409", description = "Duplicate username or email",
                         content = @Content(schema = @Schema(implementation = ErrorResponseWrapper.class)))
    })
    @PostMapping("/signup")
    @ResponseStatus(HttpStatus.CREATED)
    public BaseResponseWrapper<UserDTO> signup(@Valid @RequestBody UsernamePasswordSignupRequest userRequest) {
        UserDTO data = usersService.signup(userRequest);
        return ResponseWrapperBuilder.withData(HttpStatus.CREATED, "User created successfully", data);
    }

    @Operation(summary = "Update user information",
               security = @SecurityRequirement(name = "Bearer Authentication"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Updated successfully", useReturnTypeSchema = true),
            @ApiResponse(responseCode = "400", description = "Invalid request",
                         content = @Content(schema = @Schema(implementation = InvalidResponseWrapper.class))
            ),
    })
    @PatchMapping("/update")
    @ResponseStatus(HttpStatus.OK)
    public BaseResponseWrapper<UserDTO> updateUserInfo(@Valid @RequestBody UserUpdateRequest userRequest,
                                                       @AuthenticationPrincipal CustomUserDetails userDetails) {
        String username = userDetails.getUsername();
        UserDTO dto = usersService.update(userRequest, username);
        return ResponseWrapperBuilder.withData(HttpStatus.OK, "User updated successfully", dto);
    }
}
