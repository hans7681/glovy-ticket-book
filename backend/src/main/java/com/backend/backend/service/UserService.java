package com.backend.backend.service;

import com.backend.backend.dto.LoginRequest;
import com.backend.backend.dto.RegisterRequest;
import com.backend.backend.entity.User;
import com.baomidou.mybatisplus.extension.service.IService;

public interface UserService extends IService<User> {
    User register(RegisterRequest registerRequest);
    String login(LoginRequest loginRequest); // 登录成功返回 JWT
    User findByUsername(String username);
    // findById 方法由 IService 提供 (getById)，无需在此声明

    // TODO: 添加管理员更新用户信息的方法声明
    boolean updateUserByAdmin(Long userId, User userUpdateRequest);
    // TODO: 添加管理员删除/禁用用户的方法声明
    // boolean disableUser(Long userId);

    // --- 用户个人资料管理 ---

    /**
     * 获取当前登录用户的个人资料。
     *
     * @param userId 当前登录用户的 ID
     * @return 用户实体，如果找不到则返回 null
     */
    User getUserProfileById(Long userId);

    /**
     * 更新当前登录用户的个人资料。
     *
     * @param userId 当前登录用户的 ID
     * @param updateDto 包含要更新的信息的 DTO
     * @return 更新后的用户实体
     * @throws com.backend.backend.exception.ResourceNotFoundException 如果用户不存在
     * @throws com.backend.backend.exception.ConflictException 如果手机号或邮箱已被其他用户占用
     */
    User updateUserProfile(Long userId, com.backend.backend.dto.UpdateProfileRequestDTO updateDto);

    /**
     * 修改当前登录用户的密码。
     *
     * @param userId 当前登录用户的 ID
     * @param changePasswordDto 包含当前密码和新密码的 DTO
     * @throws com.backend.backend.exception.ResourceNotFoundException 如果用户不存在
     * @throws com.backend.backend.exception.InvalidCredentialsException 如果当前密码不正确
     * @throws com.backend.backend.exception.ValidationException 如果新密码和确认密码不匹配或不符合复杂度要求
     */
    void changePassword(Long userId, com.backend.backend.dto.ChangePasswordRequestDTO changePasswordDto);
} 