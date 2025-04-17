package com.backend.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * DTO for changing user password.
 */
@Data
@Schema(description = "修改密码请求")
public class ChangePasswordRequestDTO {

    @NotBlank(message = "当前密码不能为空")
    @Schema(description = "当前使用的密码", required = true, example = "oldPassword123")
    private String currentPassword;

    @NotBlank(message = "新密码不能为空")
    @Size(min = 6, max = 100, message = "新密码长度必须在 6 到 100 位之间")
    @Schema(description = "设置的新密码 (长度6-100)", required = true, example = "newPassword456")
    private String newPassword;

    @NotBlank(message = "确认密码不能为空")
    @Schema(description = "确认新密码", required = true, example = "newPassword456")
    private String confirmPassword;
} 