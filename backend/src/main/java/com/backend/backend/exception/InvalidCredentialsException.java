package com.backend.backend.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Exception indicating invalid credentials provided, typically incorrect password.
 * Maps to HTTP 400 Bad Request (as it's often a client error during an operation like password change).
 * Could also map to 401 Unauthorized in a pure login context.
 */
@ResponseStatus(HttpStatus.BAD_REQUEST) // Or HttpStatus.UNAUTHORIZED depending on context
public class InvalidCredentialsException extends RuntimeException {

    public InvalidCredentialsException(String message) {
        super(message);
    }

    public InvalidCredentialsException(String message, Throwable cause) {
        super(message, cause);
    }
} 