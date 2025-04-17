package com.backend.backend.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Exception thrown when an operation is attempted on a resource 
 * that is in an inappropriate state for that operation.
 * Example: Trying to mark a 'CANCELLED' order as 'PAID'.
 */
@ResponseStatus(HttpStatus.BAD_REQUEST) // 400 Bad Request seems suitable
public class InvalidOperationException extends RuntimeException {

    public InvalidOperationException(String message) {
        super(message);
    }

    public InvalidOperationException(String message, Throwable cause) {
        super(message, cause);
    }
} 