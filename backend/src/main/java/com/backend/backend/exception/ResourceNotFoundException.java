package com.backend.backend.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Generic exception for cases where a requested resource (e.g., Order, Screening, User) is not found.
 */
@ResponseStatus(HttpStatus.NOT_FOUND) // Maps to 404 Not Found
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }

    public ResourceNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
} 