package com.diepnn.shortenurl.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * The wrapper for the response
 */

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class BaseResponseWrapper<T> implements Serializable {
    private LocalDateTime timestamp;
    private int status;
    private String message;
    private T data;
    private List<ErrorDetail> errors;

    public BaseResponseWrapper() {
        this.timestamp = LocalDateTime.now();
    }
}
