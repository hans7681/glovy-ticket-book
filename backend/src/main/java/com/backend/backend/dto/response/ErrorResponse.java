package com.backend.backend.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 标准API错误响应体
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL) // 不包含 null 值的字段
public class ErrorResponse {

    private LocalDateTime timestamp;
    private int status;
    private String error; // HTTP 状态码对应的文本，如 "Bad Request"
    private String message; // 具体的错误信息
    private String path; // 请求路径
    private List<FieldErrorDetail> fieldErrors; // 字段验证错误列表 (可选)

    public ErrorResponse(int status, String error, String message, String path) {
        this.timestamp = LocalDateTime.now();
        this.status = status;
        this.error = error;
        this.message = message;
        this.path = path;
    }

     public ErrorResponse(int status, String error, String message, String path, List<FieldErrorDetail> fieldErrors) {
        this(status, error, message, path);
        this.fieldErrors = fieldErrors;
    }
} 