package com.diepnn.shortenurl.controller;

import com.diepnn.shortenurl.dto.UserInfo;
import com.diepnn.shortenurl.dto.response.ErrorResponseWrapper;
import com.diepnn.shortenurl.dto.response.InvalidResponseWrapper;
import com.diepnn.shortenurl.service.ResolveUrlService;
import com.diepnn.shortenurl.utils.UserInfoRequestExtractor;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

/**
 * The controller for resolving the shortened URL to its original URL.
 */
@RestController
@RequiredArgsConstructor
@Tag(name = "Resolve shorten URL", description = "Main functions")
public class ResolveUrlController {
    private final ResolveUrlService resolveUrlService;
    private final UserInfoRequestExtractor userInfoRequestExtractor;

    /**
     * Resolves the given short URL to its original.
     *
     * @param shortUrl shorten URL (required)
     */
    @Operation(summary = "Access short URL and redirect to original")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "307", description = "Temporary redirect to the original URL",
                         headers = {
                                 @Header(name = HttpHeaders.LOCATION,
                                         description = "The original URL to which the user is redirected")
                         }),
            @ApiResponse(responseCode = "400", description = "Invalid short URL",
                         content = @Content(schema = @Schema(implementation = InvalidResponseWrapper.class))),
            @ApiResponse(responseCode = "404", description = "Shorten URL not found",
                         content = @Content(schema = @Schema(implementation = ErrorResponseWrapper.class)))
    })
    @GetMapping("/{shortUrl:[A-Za-z0-9-]+}")
    public ResponseEntity<Void> access(@PathVariable String shortUrl, HttpServletRequest request) {
        UserInfo userInfo = userInfoRequestExtractor.getUserInfo(request);
        String originalUrl = resolveUrlService.resolve(shortUrl, userInfo);
        return ResponseEntity.status(HttpStatus.TEMPORARY_REDIRECT)
                             .location(URI.create(originalUrl))
                             .build();
    }
}
