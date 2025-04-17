package com.backend.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
 
@Data
public class LoginRequest {
    @NotBlank(message = "用户名不能为空")
    private String username;

    @NotBlank(message = "密码不能为空")
    //@Size(min = 6, message = "密码长度至少为6位") // 登录时不校验密码长度，只校验非空
    private String password;
} 