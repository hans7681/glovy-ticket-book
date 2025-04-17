package com.backend.backend.controller;

import com.backend.backend.entity.User;
import com.backend.backend.service.UserService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

@Tag(name = "系统管理员 - 用户管理", description = "系统管理员管理用户信息接口 (需要系统管理员权限)")
@RestController
@RequestMapping("/api/admin/users")
@Slf4j
public class AdminUserController {

    @Autowired
    private UserService userService;

    @Operation(
        summary = "获取用户列表 (分页)", 
        description = "获取系统用户列表，可按用户名或角色筛选。返回分页结果，包含用户基本信息（不含密码）。"
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200", 
            description = "成功获取用户列表", 
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = Page.class),
                examples = @ExampleObject(
                    name = "用户列表示例",
                    value = "{\n" +
                           "  \"records\": [\n" +
                           "    {\n" +
                           "      \"id\": 1,\n" +
                           "      \"username\": \"admin\",\n" +
                           "      \"nickname\": \"系统管理员\",\n" +
                           "      \"phone\": \"13800000000\",\n" +
                           "      \"email\": \"admin@example.com\",\n" +
                           "      \"role\": \"SYSTEM_ADMIN\",\n" +
                           "      \"createTime\": \"2023-01-01T12:00:00\"\n" +
                           "    }\n" +
                           "  ],\n" +
                           "  \"total\": 1,\n" +
                           "  \"size\": 10,\n" +
                           "  \"current\": 1,\n" +
                           "  \"pages\": 1\n" +
                           "}"
                )
            )
        ),
        @ApiResponse(
            responseCode = "401", 
            description = "未授权，需要登录",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    value = "{\n" +
                           "  \"message\": \"未授权，请先登录\",\n" +
                           "  \"code\": 401\n" +
                           "}"
                )
            )
        ),
        @ApiResponse(
            responseCode = "403", 
            description = "权限不足，需要系统管理员权限",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    value = "{\n" +
                           "  \"message\": \"权限不足，需要系统管理员权限\",\n" +
                           "  \"code\": 403\n" +
                           "}"
                )
            )
        )
    })
    @GetMapping
    public ResponseEntity<Page<User>> listUsers(
            @Parameter(description = "当前页码", example = "1", schema = @Schema(minimum = "1"), in = ParameterIn.QUERY) 
            @RequestParam(defaultValue = "1") int current,
            
            @Parameter(description = "每页条数", example = "10", schema = @Schema(minimum = "1", maximum = "100"), in = ParameterIn.QUERY) 
            @RequestParam(defaultValue = "10") int size,
            
            @Parameter(description = "按用户名模糊搜索，支持部分匹配", example = "admin", in = ParameterIn.QUERY) 
            @RequestParam(required = false) String username,
            
            @Parameter(
                description = "按角色筛选 (USER: 普通用户, CINEMA_ADMIN: 影院管理员, SYSTEM_ADMIN: 系统管理员)", 
                schema = @Schema(implementation = User.UserRole.class, 
                               enumAsRef = true,
                               example = "SYSTEM_ADMIN"), 
                in = ParameterIn.QUERY
            )
            @RequestParam(required = false) User.UserRole role) {
        
        Page<User> page = new Page<>(current, size);
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        
        if (StringUtils.hasText(username)) {
            wrapper.like(User::getUsername, username); // 模糊查询
        }
        
        if (role != null) {
            wrapper.eq(User::getRole, role);
        }
        
        wrapper.orderByDesc(User::getCreateTime);
        // 注意：密码等敏感信息不应返回给前端，需要在 Service 或 Controller 层处理
        Page<User> resultPage = userService.page(page, wrapper);
        // 清理敏感信息
        resultPage.getRecords().forEach(user -> user.setPassword(null));
        
        return ResponseEntity.ok(resultPage);
    }

    @Operation(
        summary = "获取用户详情", 
        description = "根据用户 ID 获取用户详细信息，包括基本资料但不包含密码。"
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200", 
            description = "成功获取用户详情", 
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = User.class),
                examples = @ExampleObject(
                    value = "{\n" +
                           "  \"id\": 1,\n" +
                           "  \"username\": \"admin\",\n" +
                           "  \"nickname\": \"系统管理员\",\n" +
                           "  \"phone\": \"13800000000\",\n" +
                           "  \"email\": \"admin@example.com\",\n" +
                           "  \"role\": \"SYSTEM_ADMIN\",\n" +
                           "  \"createTime\": \"2023-01-01T12:00:00\",\n" +
                           "  \"updateTime\": \"2023-01-01T12:00:00\"\n" +
                           "}"
                )
            )
        ),
        @ApiResponse(
            responseCode = "404", 
            description = "用户不存在",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    value = "{\n" +
                           "  \"message\": \"用户不存在\",\n" +
                           "  \"code\": 404\n" +
                           "}"
                )
            )
        ),
        @ApiResponse(responseCode = "401", description = "未授权，需要登录"),
        @ApiResponse(responseCode = "403", description = "权限不足，需要系统管理员权限")
    })
    @GetMapping("/{id}")
    public ResponseEntity<User> getUserById(
            @Parameter(
                description = "用户 ID", 
                required = true,
                example = "1",
                schema = @Schema(type = "integer", format = "int64")
            ) 
            @PathVariable Long id) {
            
        User user = userService.getById(id); // 使用 getById
        if (user != null) {
            user.setPassword(null); // 清理密码
            return ResponseEntity.ok(user);
        }
        return ResponseEntity.notFound().build();
    }

    @Operation(
        summary = "更新用户信息", 
        description = "更新用户的昵称、手机、邮箱、角色等信息。不能通过此接口修改密码，密码修改应使用专用接口。"
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200", 
            description = "更新成功，返回更新后的用户信息", 
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = User.class)
            )
        ),
        @ApiResponse(
            responseCode = "400", 
            description = "请求参数错误，例如手机号或邮箱格式不正确",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    value = "{\n" +
                           "  \"message\": \"手机号格式不正确\",\n" +
                           "  \"code\": 400\n" +
                           "}"
                )
            )
        ),
        @ApiResponse(
            responseCode = "409", 
            description = "资源冲突，例如手机号或邮箱已被其他用户使用",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    value = "{\n" +
                           "  \"message\": \"该邮箱已被使用\",\n" +
                           "  \"code\": 409\n" +
                           "}"
                )
            )
        ),
        @ApiResponse(responseCode = "401", description = "未授权，需要登录"),
        @ApiResponse(responseCode = "403", description = "权限不足，需要系统管理员权限"),
        @ApiResponse(responseCode = "404", description = "用户不存在")
    })
    @PutMapping("/{id}")
    public ResponseEntity<?> updateUser(
            @Parameter(
                description = "要更新的用户 ID", 
                required = true,
                example = "1",
                schema = @Schema(type = "integer", format = "int64")
            ) 
            @PathVariable Long id,
            
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                description = "需要更新的用户信息。用户名不可修改，密码字段将被忽略。",
                required = true,
                content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = User.class),
                    examples = @ExampleObject(
                        value = "{\n" +
                               "  \"nickname\": \"新昵称\",\n" +
                               "  \"phone\": \"13800000001\",\n" +
                               "  \"email\": \"newemail@example.com\",\n" +
                               "  \"role\": \"CINEMA_ADMIN\"\n" +
                               "}"
                    )
                )
            )
            @RequestBody User userUpdateRequest) {
            
        // TODO: 实现 UserService.updateUserByAdmin 方法，处理更新逻辑
        // 需要注意：不允许修改密码、用户名、创建时间等
        // Service 层需要校验手机、邮箱是否已被占用
        boolean success = userService.updateUserByAdmin(id, userUpdateRequest);

        if (success) {
             User updatedUser = userService.getById(id); // 使用 getById
             if(updatedUser != null) updatedUser.setPassword(null);
             return ResponseEntity.ok(updatedUser);
         } else {
             // Check if user exists to differentiate errors
             User existingUser = userService.getById(id);
             if (existingUser == null) {
                 // User truly doesn't exist
                 return ResponseEntity.status(HttpStatus.NOT_FOUND).body("用户不存在");
             } else {
                 // User exists, but updateById returned false (likely no changes needed)
                 // Treat this as success, return the existing (unchanged) user data.
                 log.warn("Update request for user {} processed, but no fields were changed.", id);
                 existingUser.setPassword(null); // Clear password before returning
                 return ResponseEntity.ok(existingUser);
             }
         }
    }

    @Operation(
        summary = "删除用户", 
        description = "根据 ID 删除用户账号。实际操作为逻辑删除（标记用户为禁用状态），不会物理删除数据。"
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "204", 
            description = "删除成功，无返回内容"
        ),
        @ApiResponse(
            responseCode = "400", 
            description = "操作失败，例如用户有关联的进行中订单",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    value = "{\n" +
                           "  \"message\": \"该用户有未完成的订单，无法删除\",\n" +
                           "  \"code\": 400\n" +
                           "}"
                )
            )
        ),
        @ApiResponse(responseCode = "401", description = "未授权，需要登录"),
        @ApiResponse(responseCode = "403", description = "权限不足，需要系统管理员权限"),
        @ApiResponse(responseCode = "404", description = "用户不存在")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(
            @Parameter(
                description = "要删除的用户 ID", 
                required = true,
                example = "1",
                schema = @Schema(type = "integer", format = "int64")
            ) 
            @PathVariable Long id) {
            
        // TODO: 实现 UserService.deleteUser 或 disableUser 方法
        // 考虑逻辑删除（添加 is_deleted 字段）或禁用状态
        // 删除前检查关联数据（如订单）
        boolean success = userService.removeById(id); // 物理删除示例，不推荐
        return success ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
    }

} 