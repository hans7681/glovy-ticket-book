package com.backend.backend.dto;

import com.backend.backend.entity.User;
import com.backend.backend.entity.User.UserRole;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO for returning user profile information. Excludes sensitive fields like password.
 */
@Data
@NoArgsConstructor
@Schema(description = "用户个人资料信息 (安全)")
public class UserProfileDTO {

    @Schema(description = "用户 ID", example = "5")
    private Long id;

    @Schema(description = "用户名 (登录账号)", example = "user123")
    private String username;

    @Schema(description = "昵称", example = "小明")
    private String nickname;

    @Schema(description = "头像 URL", example = "/avatars/default.png")
    private String avatar;

    @Schema(description = "手机号码", example = "13800138000")
    private String phone;

    @Schema(description = "电子邮箱", example = "user@example.com")
    private String email;

    @Schema(description = "用户角色", example = "USER")
    private UserRole role;

    @Schema(description = "账号创建时间")
    private LocalDateTime createTime;

    @Schema(description = "最后更新时间")
    private LocalDateTime updateTime;

    /**
     * Convenience constructor to create DTO from User entity.
     * @param user The User entity
     */
    public UserProfileDTO(User user) {
        if (user != null) {
            this.id = user.getId();
            this.username = user.getUsername();
            this.nickname = user.getNickname();
            this.avatar = user.getAvatar();
            this.phone = user.getPhone();
            this.email = user.getEmail();
            this.role = user.getRole();
            this.createTime = user.getCreateTime();
            this.updateTime = user.getUpdateTime();
        }
    }

    // Static factory method might be another good pattern
    public static UserProfileDTO fromUser(User user) {
        return new UserProfileDTO(user);
    }
} 