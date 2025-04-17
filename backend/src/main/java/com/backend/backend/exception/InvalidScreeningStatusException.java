package com.backend.backend.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Exception thrown when attempting an operation on a screening whose status is invalid for that operation (e.g., creating an order for a non-approved screening).
 */
@ResponseStatus(HttpStatus.BAD_REQUEST) // 400 Bad Request seems suitable for invalid status
public class InvalidScreeningStatusException extends RuntimeException {

    public InvalidScreeningStatusException(String message) {
        super(message);
    }

    public InvalidScreeningStatusException(String message, Throwable cause) {
        super(message, cause);
    }
} 