package com.diepnn.shortenurl.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * Return when there is an error occurs, used for swagger documentation, it will hide the {@code data} and {@code errors} field.
 */
public class ErrorResponseWrapper extends BaseResponseWrapper<Void> {
    @Schema(hidden = true)
    private Object data;

    @Schema(hidden = true)
    private List<ErrorDetail> errors;
}
