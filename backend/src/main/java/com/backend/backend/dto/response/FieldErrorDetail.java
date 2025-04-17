package com.backend.backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 字段验证错误详情
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FieldErrorDetail {
    private String field;       // 错误的字段名
    private String message;     // 错误信息
    private Object rejectedValue; // 被拒绝的值 (可选)
} 