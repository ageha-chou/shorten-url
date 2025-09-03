package com.diepnn.shortenurl.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

public class InvalidResponseWrapper extends BaseResponseWrapper {
    @Schema(hidden = true)
    private Object data;
}
