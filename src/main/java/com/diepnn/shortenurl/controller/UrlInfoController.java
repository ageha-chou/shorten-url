package com.diepnn.shortenurl.controller;

import com.diepnn.shortenurl.dto.UrlInfoDTO;
import com.diepnn.shortenurl.dto.UserInfo;
import com.diepnn.shortenurl.dto.request.UrlInfoRequest;
import com.diepnn.shortenurl.dto.response.BaseResponseWrapper;
import com.diepnn.shortenurl.dto.response.InvalidResponseWrapper;
import com.diepnn.shortenurl.entity.UrlInfo;
import com.diepnn.shortenurl.exception.TooManyRequestException;
import com.diepnn.shortenurl.mapper.UrlInfoMapper;
import com.diepnn.shortenurl.mapper.translator.ShortUrlMappings;
import com.diepnn.shortenurl.service.UrlInfoService;
import com.diepnn.shortenurl.utils.ResponseWrapperBuilder;
import com.diepnn.shortenurl.utils.UserInfoRequestExtractor;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * The controller for managing URL information.
 */
@RestController
@RequestMapping("/api/v1/url-infos")
@RequiredArgsConstructor
@Tag(name = "URL Info", description = "URL Info API")
public class UrlInfoController {
    private final UrlInfoService urlInfoService;
    private final UserInfoRequestExtractor userInfoRequestExtractor;
    private final UrlInfoMapper urlInfoMapper;
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
            )
    })
    @PostMapping(path = "/create")
    @ResponseStatus(HttpStatus.CREATED)
    public BaseResponseWrapper<UrlInfoDTO> create(@Valid @RequestBody UrlInfoRequest userRequest, HttpServletRequest request) throws TooManyRequestException {
        shortUrlMappings.validateOriginalUrl(userRequest.getOriginalUrl());
        userRequest.setOriginalUrl(shortUrlMappings.normalizeUrl(userRequest.getOriginalUrl()));
        UserInfo userInfo = userInfoRequestExtractor.getUserInfo(request);
        UrlInfo shortenUrl = urlInfoService.create(userRequest, userInfo);
        UrlInfoDTO dto = urlInfoMapper.toDto(shortenUrl);
        return ResponseWrapperBuilder.withData(HttpStatus.CREATED, "Shorten URL created successfully", dto);
    }
}
