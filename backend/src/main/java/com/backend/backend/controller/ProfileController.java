package com.backend.backend.controller;

import com.backend.backend.dto.ChangePasswordRequestDTO;
import com.backend.backend.dto.UpdateProfileRequestDTO;
import com.backend.backend.dto.UserProfileDTO;
import com.backend.backend.entity.User;
import com.backend.backend.exception.*; // Import custom exceptions (assuming a base package)
import com.backend.backend.service.UserService;
import com.backend.backend.util.SecurityUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/profile")
@RequiredArgsConstructor
@Tag(name = "用户 - 个人资料管理", description = "管理用户自己的个人信息")
@SecurityRequirement(name = "JWT") // Indicate that JWT is required for this controller
public class ProfileController {

    private final UserService userService;

    // Helper method for consistent error responses (consider moving to a BaseController)
    private java.util.Map<String, String> errorResponse(String message) {
        return java.util.Collections.singletonMap("message", message);
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "获取当前用户个人资料", description = "获取当前登录用户的详细个人信息（不含密码）。")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "成功获取个人资料", content = @Content(schema = @Schema(implementation = UserProfileDTO.class))),
            @ApiResponse(responseCode = "401", description = "未授权，需要登录", content = @Content),
            @ApiResponse(responseCode = "404", description = "用户不存在（理论上不应发生，因为是基于认证用户）", content = @Content)
    })
    public ResponseEntity<?> getCurrentUserProfile() {
        Long currentUserId = SecurityUtil.getCurrentUserId();
        if (currentUserId == null) {
            // This should technically be caught by @PreAuthorize, but defensive check
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse("无法获取当前用户信息"));
        }

        User user = userService.getUserProfileById(currentUserId);
        if (user == null) {
            log.error("Could not find user profile for authenticated user ID: {}", currentUserId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse("未找到用户个人资料"));
        }

        return ResponseEntity.ok(UserProfileDTO.fromUser(user));
    }

    @PutMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "更新当前用户个人资料", description = "更新当前登录用户的昵称、头像、手机或邮箱。用户名和角色不可修改。")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "更新成功", content = @Content(schema = @Schema(implementation = UserProfileDTO.class))),
            @ApiResponse(responseCode = "400", description = "请求参数无效（如格式错误）", content = @Content(schema = @Schema(implementation = java.util.Map.class))),
            @ApiResponse(responseCode = "401", description = "未授权，需要登录", content = @Content),
            @ApiResponse(responseCode = "404", description = "用户不存在（理论上不应发生）", content = @Content(schema = @Schema(implementation = java.util.Map.class))),
            @ApiResponse(responseCode = "409", description = "资源冲突（手机号或邮箱已被其他用户占用）", content = @Content(schema = @Schema(implementation = java.util.Map.class))),
            @ApiResponse(responseCode = "500", description = "服务器内部错误", content = @Content(schema = @Schema(implementation = java.util.Map.class)))
    })
    public ResponseEntity<?> updateCurrentUserProfile(
            @Parameter(description = "包含要更新的字段（昵称、头像、手机、邮箱）", required = true)
            @Valid @RequestBody UpdateProfileRequestDTO updateDto) {
        Long currentUserId = SecurityUtil.getCurrentUserId();
        if (currentUserId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse("无法获取当前用户信息"));
        }

        try {
            User updatedUser = userService.updateUserProfile(currentUserId, updateDto);
            return ResponseEntity.ok(UserProfileDTO.fromUser(updatedUser));
        } catch (ResourceNotFoundException e) { // Assuming this is thrown if user not found
            log.warn("Update profile failed for user {}: {}", currentUserId, e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse(e.getMessage()));
        } catch (ConflictException e) { // Assuming this for uniqueness violation
            log.warn("Update profile conflict for user {}: {}", currentUserId, e.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT).body(errorResponse(e.getMessage()));
        } catch (Exception e) {
            log.error("Unexpected error updating profile for user {}: {}", currentUserId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse("更新个人资料时发生内部错误"));
        }
    }

    @PutMapping("/password")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "修改当前用户密码", description = "验证旧密码并设置新密码。")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "密码修改成功", content = @Content),
            @ApiResponse(responseCode = "400", description = "请求无效（如密码不匹配、格式错误、旧密码错误）", content = @Content(schema = @Schema(implementation = java.util.Map.class))),
            @ApiResponse(responseCode = "401", description = "未授权，需要登录", content = @Content),
            @ApiResponse(responseCode = "404", description = "用户不存在（理论上不应发生）", content = @Content(schema = @Schema(implementation = java.util.Map.class))),
            @ApiResponse(responseCode = "500", description = "服务器内部错误", content = @Content(schema = @Schema(implementation = java.util.Map.class)))
    })
    public ResponseEntity<?> changeCurrentUserPassword(
            @Parameter(description = "包含当前密码、新密码和确认密码", required = true)
            @Valid @RequestBody ChangePasswordRequestDTO changePasswordDto) {
        Long currentUserId = SecurityUtil.getCurrentUserId();
        if (currentUserId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse("无法获取当前用户信息"));
        }

        try {
            userService.changePassword(currentUserId, changePasswordDto);
            return ResponseEntity.noContent().build(); // 204 No Content on successful password change
        } catch (ResourceNotFoundException e) {
            log.warn("Change password failed for user {}: {}", currentUserId, e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse(e.getMessage()));
        } catch (InvalidCredentialsException e) { // Assuming for incorrect current password
            log.warn("Change password failed for user {} due to invalid credentials.", currentUserId);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse(e.getMessage()));
        } catch (ValidationException e) { // Assuming for new password mismatch or complexity issues
            log.warn("Change password failed for user {} due to validation error: {}", currentUserId, e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse(e.getMessage()));
        } catch (Exception e) {
            log.error("Unexpected error changing password for user {}: {}", currentUserId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse("修改密码时发生内部错误"));
        }
    }
} 