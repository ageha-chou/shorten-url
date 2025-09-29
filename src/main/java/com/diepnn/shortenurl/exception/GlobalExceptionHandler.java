package com.diepnn.shortenurl.exception;

import com.diepnn.shortenurl.dto.response.BaseResponseWrapper;
import com.diepnn.shortenurl.dto.response.ErrorDetail;
import com.diepnn.shortenurl.utils.ResponseWrapperBuilder;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.NoHandlerFoundException;

import java.util.ArrayList;
import java.util.List;

/**
 * The global exception handler for all exceptions
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {
    /**
     * Handle constraint violation exception (e.g. @RequestParam + @PathVariable)
     *
     * @param ex the exception to handle
     * @return wrapped response
     */
    @ExceptionHandler(ConstraintViolationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public BaseResponseWrapper<Void> handleConstraintViolation(ConstraintViolationException ex) {
        List<ErrorDetail> errors = new ArrayList<>();
        ex.getConstraintViolations().forEach(violation -> {
            String fieldName = camelToSnake(violation.getPropertyPath().toString());
            String errorMessage = violation.getMessage();
            errors.add(new ErrorDetail(fieldName, errorMessage));
        });

        return ResponseWrapperBuilder.withError(HttpStatus.BAD_REQUEST, "Invalid request", errors);
    }

    /**
     * Handle method argument not valid exception (e.g. @RequestBody)
     *
     * @param ex the exception to handle
     * @return wrapped response
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public BaseResponseWrapper<Void> handleMethodArgumentNotValidException(MethodArgumentNotValidException ex) {
        List<ErrorDetail> errors = new ArrayList<>();
        ex.getBindingResult().getFieldErrors().forEach(fieldError -> {
            String fieldName = camelToSnake(fieldError.getField());
            String errorMessage = fieldError.getDefaultMessage();
            errors.add(new ErrorDetail(fieldName, errorMessage));
        });

        return ResponseWrapperBuilder.withError(HttpStatus.BAD_REQUEST, "Invalid request", errors);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public BaseResponseWrapper<Void> handleIllegalArgumentException(IllegalArgumentException ex) {
        return ResponseWrapperBuilder.withNoData(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public BaseResponseWrapper<Void> handleHttpMessageNotReadableException(HttpMessageNotReadableException ex) {
        return ResponseWrapperBuilder.withNoData(HttpStatus.BAD_REQUEST, "Invalid request");
    }

    @ExceptionHandler(NoHandlerFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public BaseResponseWrapper<Void> handleNoHandlerFoundException(NoHandlerFoundException ex) {
        return ResponseWrapperBuilder.withNoData(HttpStatus.NOT_FOUND, "URL not found");
    }

    @ExceptionHandler(NotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public BaseResponseWrapper<Void> handleNotFoundException(NotFoundException ex) {
        return ResponseWrapperBuilder.withNoData(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(DuplicateUniqueKeyException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public BaseResponseWrapper<Void> handleDuplicateConstraintException(RuntimeException ex) {
        return ResponseWrapperBuilder.withNoData(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(TooManyRequestException.class)
    @ResponseStatus(HttpStatus.TOO_MANY_REQUESTS)
    public BaseResponseWrapper<Void> handleNoHandlerFoundException(TooManyRequestException ex) {
        return ResponseWrapperBuilder.withNoData(HttpStatus.TOO_MANY_REQUESTS, ex.getMessage());
    }

    @ExceptionHandler(IdCollisionException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public BaseResponseWrapper<Void> handleIdCollisionException(IdCollisionException ex) {
        return ResponseWrapperBuilder.withNoData(HttpStatus.INTERNAL_SERVER_ERROR, ex.getMessage());
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public BaseResponseWrapper<Void> handleException(Exception ex, WebRequest request) {
        log.error("Unhandled exception at [{} {}]",
                  request.getDescription(false), request, ex);
        return ResponseWrapperBuilder.withNoData(HttpStatus.INTERNAL_SERVER_ERROR, "Something went wrong");
    }

    private String camelToSnake(String fieldName) {
        return fieldName.replaceAll("([a-z])([A-Z]+)", "$1_$2").toLowerCase();
    }
}
