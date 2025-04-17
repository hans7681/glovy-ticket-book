package com.backend.backend.controller;

import com.backend.backend.dto.LoginRequest;
import com.backend.backend.dto.RegisterRequest;
import com.backend.backend.entity.User;
import com.backend.backend.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.HashMap;
import java.util.Map;

@Tag(name = "认证", description = "用户注册与登录接口")
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private UserService userService;

    @Operation(
        summary = "用户注册", 
        description = "注册新用户账号。默认注册的用户角色为普通用户(USER)。用户名必须唯一，密码长度至少为6位。"
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "201", 
            description = "注册成功", 
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = RegisterResponse.class),
                examples = @ExampleObject(
                    value = "{\n" +
                           "  \"message\": \"用户注册成功\",\n" +
                           "  \"userId\": 123,\n" +
                           "  \"username\": \"newuser\"\n" +
                           "}"
                )
            )
        ),
        @ApiResponse(
            responseCode = "400", 
            description = "注册失败，例如用户名已存在或密码不符合要求",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    value = "{\n" +
                           "  \"message\": \"用户名已存在\",\n" +
                           "  \"code\": 400\n" +
                           "}"
                )
            )
        )
    })
    @PostMapping("/register")
    public ResponseEntity<?> registerUser(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                description = "用户注册信息",
                required = true,
                content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = RegisterRequest.class),
                    examples = @ExampleObject(
                        value = "{\n" +
                               "  \"username\": \"newuser\",\n" +
                               "  \"password\": \"password123\",\n" +
                               "  \"nickname\": \"新用户\",\n" +
                               "  \"phone\": \"13800138000\",\n" +
                               "  \"email\": \"user@example.com\"\n" +
                               "}"
                    )
                )
            )
            @Valid @RequestBody RegisterRequest registerRequest) {
        User registeredUser = userService.register(registerRequest);
        // 使用专门的 Response DTO 更规范
        RegisterResponse response = new RegisterResponse("用户注册成功", registeredUser.getId(), registeredUser.getUsername());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(
        summary = "用户登录", 
        description = "使用用户名和密码登录，成功后获取JWT令牌。该令牌需要在后续请求的Authorization头中使用Bearer方案。"
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200", 
            description = "登录成功，返回JWT令牌", 
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = LoginResponse.class),
                examples = @ExampleObject(
                    value = "{\n" +
                           "  \"token\": \"eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJ1c2VyMSIsImlhdCI6MTY1MTIzNDU2NywiZXhwIjoxNjUxMjM4MTY3fQ.abcdefghijklmnopqrstuvwxyz\"\n" +
                           "}"
                )
            )
        ),
        @ApiResponse(
            responseCode = "401", 
            description = "登录失败，用户名或密码错误",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    value = "{\n" +
                           "  \"message\": \"用户名或密码错误\",\n" +
                           "  \"code\": 401\n" +
                           "}"
                )
            )
        )
    })
    @PostMapping("/login")
    public ResponseEntity<?> loginUser(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                description = "用户登录信息",
                required = true,
                content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = LoginRequest.class),
                    examples = @ExampleObject(
                        value = "{\n" +
                               "  \"username\": \"user1\",\n" +
                               "  \"password\": \"password123\"\n" +
                               "}"
                    )
                )
            )
            @Valid @RequestBody LoginRequest loginRequest) {
        String token = userService.login(loginRequest);
        LoginResponse response = new LoginResponse(token);
        return ResponseEntity.ok(response);
    }

    // --- Schema DTOs for Swagger Documentation ---
    @Schema(description = "注册成功响应体")
    private record RegisterResponse(
            @Schema(description = "提示信息", example = "用户注册成功") String message,
            @Schema(description = "用户ID", example = "123") Long userId,
            @Schema(description = "用户名", example = "newuser") String username) {}

    @Schema(description = "登录成功响应体")
    private record LoginResponse(
            @Schema(description = "JWT令牌", example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...") String token) {}
} 