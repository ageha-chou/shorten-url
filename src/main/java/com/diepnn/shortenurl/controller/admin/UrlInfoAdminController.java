package com.diepnn.shortenurl.controller.admin;

import com.diepnn.shortenurl.common.constant.PagingConstants;
import com.diepnn.shortenurl.common.enums.SortDirection;
import com.diepnn.shortenurl.common.enums.UrlInfoStatus;
import com.diepnn.shortenurl.dto.UrlInfoDTO;
import com.diepnn.shortenurl.dto.filter.UrlInfoFilter;
import com.diepnn.shortenurl.dto.request.DeactivateUrlInfo;
import com.diepnn.shortenurl.dto.response.BaseResponseWrapper;
import com.diepnn.shortenurl.dto.response.ErrorResponseWrapper;
import com.diepnn.shortenurl.dto.response.InvalidResponseWrapper;
import com.diepnn.shortenurl.dto.response.PageResponse;
import com.diepnn.shortenurl.security.CustomUserDetails;
import com.diepnn.shortenurl.service.UrlInfoService;
import com.diepnn.shortenurl.utils.ResponseWrapperBuilder;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Set;

@RestController
@RequestMapping("/api/v1/admin/url-info")
@RequiredArgsConstructor
@SecurityRequirement(name = "Bearer Authentication")
@Tag(name = "URL Info Admin", description = "URL Info Admin API")
public class UrlInfoAdminController {
    private final UrlInfoService urlInfoService;

    @Operation(summary = "Get all URL info")
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
    public BaseResponseWrapper<PageResponse<UrlInfoDTO>> getAllUrlInfo(
            // --- filters ---
            @RequestParam(required = false) Set<UrlInfoStatus> statuses,
            @RequestParam(required = false) Boolean alias,
            @RequestParam(required = false) String shortCode,
            @RequestParam(required = false, name = "originalUrl") String originalUrlPattern,
            @RequestParam(required = false) Set<Long> userIds,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant createdFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant createdTo,

            // --- paging ---
            @RequestParam(defaultValue = PagingConstants.DEFAULT_PAGE) int page,
            @RequestParam(defaultValue = PagingConstants.DEFAULT_PAGE_SIZE) int size,

            // --- sorting ---
            @RequestParam(defaultValue = PagingConstants.DEFAULT_SORT_FIELD) String sortField,
            @RequestParam(defaultValue = PagingConstants.DEFAULT_SORT_DIRECTION) SortDirection sortDirection
    ) {
        UrlInfoFilter filter = new UrlInfoFilter(statuses, alias, shortCode, originalUrlPattern, userIds, createdFrom, createdTo);
        Sort.Direction direction = sortDirection == SortDirection.ASC ? Sort.Direction.ASC : Sort.Direction.DESC;

        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortField));
        Page<UrlInfoDTO> result = urlInfoService.findAllUrl(filter, pageable);
        return ResponseWrapperBuilder.withData(HttpStatus.OK, "Found", PageResponse.from(result));
    }

    @Operation(summary = "Deactivate the given URL info")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Deactivated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request",
                         content = @Content(schema = @Schema(implementation = InvalidResponseWrapper.class))
            ),
            @ApiResponse(responseCode = "404", description = "Url not found",
                         content = @Content(schema = @Schema(implementation = ErrorResponseWrapper.class))
            )
    })
    @PatchMapping("/{urlId}/deactivate")
    @ResponseStatus(HttpStatus.OK)
    public BaseResponseWrapper<Void> deactivateUrlInfo(@PathVariable Long urlId,
                                                       @RequestBody DeactivateUrlInfo userRequest,
                                                       @AuthenticationPrincipal CustomUserDetails userDetails) {
        urlInfoService.deactivateUrlInfo(urlId, userRequest, userDetails);
        return ResponseWrapperBuilder.withNoData(HttpStatus.OK, "Deactivated successfully");
    }
}
