package com.diepnn.shortenurl.controller;

import com.diepnn.shortenurl.dto.UrlInfoDTO;
import com.diepnn.shortenurl.dto.request.UrlInfoRequest;
import com.diepnn.shortenurl.dto.response.BaseResponseWrapper;
import com.diepnn.shortenurl.dto.response.ErrorResponseWrapper;
import com.diepnn.shortenurl.dto.response.InvalidResponseWrapper;
import com.diepnn.shortenurl.entity.UrlInfo;
import com.diepnn.shortenurl.mapper.BaseMapper;
import com.diepnn.shortenurl.service.UrlInfoService;
import com.diepnn.shortenurl.utils.ResponseWrapperBuilder;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.StringToClassMapItem;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@Tag(name = "Create and Resolve shorten URL", description = "Main functions")
public class UrlInfoController {
    private final UrlInfoService urlInfoService;
    private final BaseMapper<UrlInfo, UrlInfoDTO> shortenUrlMapper;

    @Operation(summary = "Create shorten URL for the original URL")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Created successfully", useReturnTypeSchema = true),
            @ApiResponse(responseCode = "400", description = "Invalid URL",
                         content = @Content(schema = @Schema(implementation = InvalidResponseWrapper.class))
            )
    })
    @PostMapping(path = "/create")
    @ResponseStatus(HttpStatus.CREATED)
    public BaseResponseWrapper<UrlInfoDTO> create(@Valid @RequestBody UrlInfoRequest userRequest) {
        UrlInfo shortenUrl = urlInfoService.create(userRequest);
        return ResponseWrapperBuilder.withData(HttpStatus.CREATED, "Shorten URL created successfully", shortenUrlMapper.toDTO(shortenUrl));
    }

    @Operation(summary = "Access short URL and redirect to original")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "307", description = "Temporary redirect to the original URL",
                         headers = {
                                 @Header(name = HttpHeaders.LOCATION,
                                         description = "The original URL to which the client is redirected")
                         }),
            @ApiResponse(responseCode = "400", description = "Invalid short URL",
                         content = @Content(schema = @Schema(implementation = InvalidResponseWrapper.class))),
            @ApiResponse(responseCode = "404", description = "Shorten URL not found",
                         content = @Content(schema = @Schema(implementation = ErrorResponseWrapper.class)))
    })
    @GetMapping("/{shortUrl:[A-Za-z0-9-]+}")
    @ResponseStatus(HttpStatus.TEMPORARY_REDIRECT)
    public void access(@PathVariable String shortUrl) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'access'");
    }
}
