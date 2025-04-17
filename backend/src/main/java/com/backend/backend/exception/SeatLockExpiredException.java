package com.backend.backend.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Exception thrown when attempting to create an order for seats whose lock has expired or does not exist for the user.
 */
@ResponseStatus(HttpStatus.CONFLICT) // 409 Conflict, as the resource (seat lock) state is no longer valid
public class SeatLockExpiredException extends RuntimeException {

    public SeatLockExpiredException(String message) {
        super(message);
    }

    public SeatLockExpiredException(String message, Throwable cause) {
        super(message, cause);
    }
} 