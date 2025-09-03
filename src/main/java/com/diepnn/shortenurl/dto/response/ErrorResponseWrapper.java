package com.diepnn.shortenurl.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

public class ErrorResponseWrapper extends BaseResponseWrapper {
    @Schema(hidden = true)
    private Object data;

    @Schema(hidden = true)
    private List<ErrorDetail> errors;
}
