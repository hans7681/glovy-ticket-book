package com.backend.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * DTO for updating user profile information.
 * Allows updating nickname, avatar, phone, and email.
 * Username, role, and password cannot be changed via this DTO.
 */
@Data
@Schema(description = "更新用户个人资料请求")
public class UpdateProfileRequestDTO {

    @Size(max = 50, message = "昵称长度不能超过50个字符")
    @Schema(description = "新的昵称", example = "明明", required = false)
    private String nickname;

    @Schema(description = "新的头像 URL", example = "/avatars/new_avatar.png", required = false)
    private String avatar; // Consider URL validation if needed

    // Optional: Add validation for phone number format if strict rules apply
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号码格式不正确")
    @Schema(description = "新的手机号码", example = "13900139000", required = false)
    private String phone;

    @Email(message = "邮箱格式不正确")
    @Size(max = 100, message = "邮箱长度不能超过100个字符")
    @Schema(description = "新的电子邮箱", example = "new.email@example.com", required = false)
    private String email;

    // Other fields like 'gender', 'bio' could be added here if needed.
} 