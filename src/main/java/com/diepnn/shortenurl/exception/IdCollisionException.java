package com.diepnn.shortenurl.exception;

/**
 * Thrown when the ID collision occurs when generating a short URL from {@link com.diepnn.shortenurl.common.generator.IdGenerator}
 */
public class IdCollisionException extends RuntimeException {
    public IdCollisionException(String message) {
        super(message);
    }
}
