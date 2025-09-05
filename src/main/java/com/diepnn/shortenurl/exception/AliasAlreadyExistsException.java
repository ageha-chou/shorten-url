package com.diepnn.shortenurl.exception;

/**
 * Exception thrown when an alias is already in use
 */
public class AliasAlreadyExistsException extends RuntimeException {
    public AliasAlreadyExistsException(String message) {
        super(message);
    }
}
