package com.diepnn.shortenurl.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Return when the request is invalid (error code 400), used for swagger documentation.
 */
public class InvalidResponseWrapper extends BaseResponseWrapper<Void> {
    @Schema(hidden = true)
    private Object data;
}
