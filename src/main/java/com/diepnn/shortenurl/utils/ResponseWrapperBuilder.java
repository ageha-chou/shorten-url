package com.diepnn.shortenurl.utils;

import com.diepnn.shortenurl.dto.response.BaseResponseWrapper;
import com.diepnn.shortenurl.dto.response.ErrorDetail;
import org.springframework.http.HttpStatus;

import java.util.List;

/**
 * The helper class to create the wrapper {@link BaseResponseWrapper}
 */
public class ResponseWrapperBuilder {
    /**
     * Create response with no data
     *
     * @param status response HTTP status
     * @param message response message
     * @return wrapped response
     */
    public static <T> BaseResponseWrapper<T> withNoData(HttpStatus status, String message) {
        BaseResponseWrapper<T> response = new BaseResponseWrapper<>();
        response.setStatus(status.value());
        response.setMessage(message);
        return response;
    }

    /**
     * Create response with data
     *
     * @param status response HTTP status
     * @param message response message
     * @param data response data
     * @return wrapped response
     */
    public static <T> BaseResponseWrapper<T> withData(HttpStatus status, String message, T data) {
        BaseResponseWrapper<T> response = new BaseResponseWrapper<>();
        response.setStatus(status.value());
        response.setMessage(message);
        response.setData(data);
        return response;
    }

    /**
     * Create response with error
     *
     * @param status response HTTP status
     * @param message response message
     * @param errors list of error occurred
     * @return wrapped response
     */
    public static <T> BaseResponseWrapper<T> withError(HttpStatus status, String message, List<ErrorDetail> errors) {
        BaseResponseWrapper<T> response = new BaseResponseWrapper<>();
        response.setStatus(status.value());
        response.setMessage(message);
        response.setErrors(errors);
        return response;
    }
}
