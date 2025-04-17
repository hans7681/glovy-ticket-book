package com.backend.backend.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Exception thrown when attempting to lock or purchase seats that are already 
 * taken (sold or locked by another user).
 */
@ResponseStatus(HttpStatus.CONFLICT) // 409 Conflict is suitable
public class SeatsUnavailableException extends RuntimeException {

    public SeatsUnavailableException(String message) {
        super(message);
    }

    public SeatsUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
} 