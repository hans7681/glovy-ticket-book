package com.backend.backend.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND) // 让 Spring MVC 自动映射此异常到 404 Not Found
public class ScreeningNotFoundException extends RuntimeException {

    public ScreeningNotFoundException(String message) {
        super(message);
    }

    public ScreeningNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
} 