package com.diepnn.shortenurl.controller;

import com.diepnn.shortenurl.dto.UrlInfoDTO;
import com.diepnn.shortenurl.dto.UserInfo;
import com.diepnn.shortenurl.dto.request.UpdateOriginalUrl;
import com.diepnn.shortenurl.dto.request.UrlInfoRequest;
import com.diepnn.shortenurl.dto.response.BaseResponseWrapper;
import com.diepnn.shortenurl.dto.response.ErrorResponseWrapper;
import com.diepnn.shortenurl.dto.response.InvalidResponseWrapper;
import com.diepnn.shortenurl.exception.TooManyRequestException;
import com.diepnn.shortenurl.mapper.translator.ShortUrlMappings;
import com.diepnn.shortenurl.security.CustomUserDetails;
import com.diepnn.shortenurl.service.UrlInfoService;
import com.diepnn.shortenurl.utils.ResponseWrapperBuilder;
import com.diepnn.shortenurl.utils.UserInfoRequestExtractor;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * The controller for managing URL information.
 */
@RestController
@RequestMapping("/api/v1/url-infos")
@RequiredArgsConstructor
@Tag(name = "URL Info", description = "URL Info API")
@SecurityRequirement(name = "Bearer Authentication")
public class UrlInfoController {
    private final UrlInfoService urlInfoService;
    private final UserInfoRequestExtractor userInfoRequestExtractor;
    private final ShortUrlMappings shortUrlMappings;

    /**
     * Creates a short URL for the given original URL.
     *
     * @param userRequest contains the original URL and alias (required)
     * @return a generated short URL
     * @throws TooManyRequestException if the number of short URLs created exceeds the limit
     */
    @Operation(summary = "Create shorten URL for the original URL")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Created successfully", useReturnTypeSchema = true),
            @ApiResponse(responseCode = "400", description = "Invalid URL",
                         content = @Content(schema = @Schema(implementation = InvalidResponseWrapper.class))
            ),
            @ApiResponse(responseCode = "429", description = "Too many request",
                         content = @Content(schema = @Schema(implementation = ErrorResponseWrapper.class))
            )
    })
    @PostMapping(path = "/create")
    @ResponseStatus(HttpStatus.CREATED)
    public BaseResponseWrapper<UrlInfoDTO> create(@Valid @RequestBody UrlInfoRequest userRequest, HttpServletRequest request,
                                                  @AuthenticationPrincipal CustomUserDetails userDetails) {
        shortUrlMappings.validateOriginalUrl(userRequest.getOriginalUrl());
        userRequest.setOriginalUrl(shortUrlMappings.normalizeUrl(userRequest.getOriginalUrl()));
        UserInfo userInfo = userInfoRequestExtractor.getUserInfo(request);

        Long userId = null;
        if (userDetails != null && userDetails.getUser() != null) {
            userId = userDetails.getId();
        }

        UrlInfoDTO dto = urlInfoService.create(userRequest, userInfo, userId);
        return ResponseWrapperBuilder.withData(HttpStatus.CREATED, "Shorten URL created successfully", dto);
    }

    /**
     * Get all URL information for the given user.
     *
     * @param userDetails an authenticated user
     * @return a list of URL information
     */
    @Operation(summary = "Get shorten URLs by user")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Found", useReturnTypeSchema = true),
            @ApiResponse(responseCode = "204", description = "Not found",
                         content = @Content(schema = @Schema(implementation = ErrorResponseWrapper.class))
            ),
            @ApiResponse(responseCode = "400", description = "Inappropriate privilege",
                         content = @Content(schema = @Schema(implementation = ErrorResponseWrapper.class))
            )
    })
    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public BaseResponseWrapper<List<UrlInfoDTO>> getByUser(@AuthenticationPrincipal CustomUserDetails userDetails) {
        Long userId = userDetails.getId();
        List<UrlInfoDTO> urlInfos = urlInfoService.findAllByUserId(userId);
        return ResponseWrapperBuilder.withData(HttpStatus.OK, "Found", urlInfos);
    }

    @Operation(summary = "Update original URL for the given short URL")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Updated successfully", useReturnTypeSchema = true),
            @ApiResponse(responseCode = "201", description = "Not found",
                         content = @Content(schema = @Schema(implementation = ErrorResponseWrapper.class))
            )
    })
    @PatchMapping("/{id}/update-original-url")
    @ResponseStatus(HttpStatus.OK)
    public BaseResponseWrapper<UrlInfoDTO> updateOriginalUrl(@Valid @RequestBody UpdateOriginalUrl userRequest,
                                                             @PathVariable Long id,
                                                             @AuthenticationPrincipal CustomUserDetails userDetails) {
        UrlInfoDTO dto = urlInfoService.updateOriginalUrl(id, userRequest, userDetails);
        return ResponseWrapperBuilder.withData(HttpStatus.OK, "Updated successfully", dto);
    }
}
