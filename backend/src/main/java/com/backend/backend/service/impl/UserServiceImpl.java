package com.backend.backend.service.impl;

import com.backend.backend.dto.LoginRequest;
import com.backend.backend.dto.RegisterRequest;
import com.backend.backend.dto.UpdateProfileRequestDTO;
import com.backend.backend.dto.ChangePasswordRequestDTO;
import com.backend.backend.entity.User;
import com.backend.backend.exception.*;
import com.backend.backend.mapper.UserMapper;
import com.backend.backend.service.UserService;
import com.backend.backend.util.JwtUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;

@Slf4j
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtUtil;

    @Override
    public User register(RegisterRequest registerRequest) {
        if (baseMapper.findByUsername(registerRequest.getUsername()) != null) {
            throw new ConflictException("用户名已存在");
        }

        User user = new User();
        user.setUsername(registerRequest.getUsername());
        user.setPassword(passwordEncoder.encode(registerRequest.getPassword()));
        user.setNickname(registerRequest.getNickname());
        user.setPhone(registerRequest.getPhone());
        user.setEmail(registerRequest.getEmail());
        user.setRole(User.UserRole.USER);

        boolean saved = this.save(user);
        if (!saved) {
            throw new RuntimeException("注册失败，请稍后重试");
        }
        return user;
    }

    @Override
    public String login(LoginRequest loginRequest) {
        User user = baseMapper.findByUsername(loginRequest.getUsername());
        if (user == null) {
            throw new InvalidCredentialsException("用户名或密码错误");
        }

        if (!passwordEncoder.matches(loginRequest.getPassword(), user.getPassword())) {
            throw new InvalidCredentialsException("用户名或密码错误");
        }

        return jwtUtil.generateToken(user.getUsername(), user.getRole().name());
    }

    @Override
    public User findByUsername(String username) {
        return query().eq("username", username).one();
    }

    // TODO: 实现管理员更新用户信息的方法 (updateUserByAdmin)
    // TODO: 实现管理员删除/禁用用户的方法 (disableUser)

    // --- 用户个人资料管理实现 ---

    @Override
    public User getUserProfileById(Long userId) {
        log.debug("Fetching profile for user ID: {}", userId);
        // getById is provided by ServiceImpl
        User user = getById(userId);
        if (user == null) {
            log.warn("User profile not found for ID: {}", userId);
            // Or throw ResourceNotFoundException here, but returning null might be acceptable
            // depending on controller handling. Controller already handles null.
        }
        return user;
    }

    @Override
    @Transactional(rollbackFor = Exception.class) // Ensure atomicity
    public User updateUserProfile(Long userId, UpdateProfileRequestDTO updateDto) {
        log.info("Attempting to update profile for user ID: {}", userId);
        User user = getById(userId);
        if (user == null) {
            log.warn("User not found for profile update: ID {}", userId);
            throw new ResourceNotFoundException("用户不存在");
        }

        boolean needsUpdate = false;

        // Check and update nickname
        if (StringUtils.hasText(updateDto.getNickname()) && !updateDto.getNickname().equals(user.getNickname())) {
            log.debug("Updating nickname for user ID: {}", userId);
            user.setNickname(updateDto.getNickname());
            needsUpdate = true;
        }

        // Check and update avatar
        if (StringUtils.hasText(updateDto.getAvatar()) && !updateDto.getAvatar().equals(user.getAvatar())) {
             log.debug("Updating avatar for user ID: {}", userId);
             user.setAvatar(updateDto.getAvatar());
             needsUpdate = true;
        }

        // Check and update email (with uniqueness check)
        if (StringUtils.hasText(updateDto.getEmail()) && !updateDto.getEmail().equalsIgnoreCase(user.getEmail())) {
            log.debug("Updating email for user ID: {}. Checking uniqueness for {}", userId, updateDto.getEmail());
            // Check if the new email is already used by another user
            User existingUserByEmail = query().eq("email", updateDto.getEmail()).ne("id", userId).one();
            if (existingUserByEmail != null) {
                log.warn("Email update conflict for user ID {}: email {} already in use.", userId, updateDto.getEmail());
                throw new ConflictException("该邮箱已被其他用户使用");
            }
            user.setEmail(updateDto.getEmail());
            needsUpdate = true;
        }

        // Check and update phone (with uniqueness check)
        if (StringUtils.hasText(updateDto.getPhone()) && !updateDto.getPhone().equals(user.getPhone())) {
            log.debug("Updating phone for user ID: {}. Checking uniqueness for {}", userId, updateDto.getPhone());
            // Check if the new phone number is already used by another user
            User existingUserByPhone = query().eq("phone", updateDto.getPhone()).ne("id", userId).one();
            if (existingUserByPhone != null) {
                log.warn("Phone update conflict for user ID {}: phone {} already in use.", userId, updateDto.getPhone());
                throw new ConflictException("该手机号已被其他用户使用");
            }
            user.setPhone(updateDto.getPhone());
            needsUpdate = true;
        }

        // Perform update only if changes were made
        if (needsUpdate) {
            log.info("Saving updated profile for user ID: {}", userId);
            user.setUpdateTime(LocalDateTime.now()); // Update timestamp
            boolean success = updateById(user);
            if (!success) {
                log.error("Failed to update user profile in database for ID: {}", userId);
                // Consider a more specific exception if needed
                throw new RuntimeException("更新个人资料失败，请稍后重试");
            }
        } else {
             log.info("No changes detected for user profile update, skipping save. User ID: {}", userId);
        }

        return user; // Return the potentially updated user
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void changePassword(Long userId, ChangePasswordRequestDTO changePasswordDto) {
        log.info("Attempting to change password for user ID: {}", userId);

        // 1. Validate new password and confirmation match
        if (!changePasswordDto.getNewPassword().equals(changePasswordDto.getConfirmPassword())) {
             log.warn("Password change validation failed for user {}: New passwords do not match.", userId);
             throw new ValidationException("新密码和确认密码不匹配");
        }

        // 2. Retrieve user
        User user = getById(userId);
        if (user == null) {
            log.warn("User not found for password change: ID {}", userId);
            throw new ResourceNotFoundException("用户不存在");
        }

        // 3. Verify current password
        if (!passwordEncoder.matches(changePasswordDto.getCurrentPassword(), user.getPassword())) {
             log.warn("Password change failed for user {}: Incorrect current password provided.", userId);
             throw new InvalidCredentialsException("当前密码不正确");
        }

        // 4. Check if new password is the same as the old one (optional but good practice)
        if (passwordEncoder.matches(changePasswordDto.getNewPassword(), user.getPassword())) {
            log.warn("Password change validation failed for user {}: New password is the same as the old password.", userId);
            throw new ValidationException("新密码不能与当前密码相同");
        }

        // 5. Encode and update password
        String encodedNewPassword = passwordEncoder.encode(changePasswordDto.getNewPassword());
        user.setPassword(encodedNewPassword);
        user.setUpdateTime(LocalDateTime.now());

        log.info("Updating password for user ID: {}", userId);
        boolean success = updateById(user);
        if (!success) {
             log.error("Failed to update password in database for ID: {}", userId);
             throw new RuntimeException("修改密码失败，请稍后重试");
        }
        log.info("Password successfully changed for user ID: {}", userId);
    }

    @Override
    @Transactional
    public boolean updateUserByAdmin(Long userId, User userUpdateRequest) {
        log.info("Admin attempting to update user ID: {}", userId);
        User existingUser = getById(userId);
        if (existingUser == null) {
            log.warn("Admin update failed: User with ID {} not found.", userId);
            // Controller should handle this based on success=false and getById=null returning 404
            return false;
        }

        boolean needsDbUpdate = false;

        // Update nickname if provided and different
        if (userUpdateRequest.getNickname() != null && !userUpdateRequest.getNickname().equals(existingUser.getNickname())) {
            log.debug("Admin updating nickname for user {}", userId);
            existingUser.setNickname(userUpdateRequest.getNickname());
            needsDbUpdate = true;
        }

        // Update phone if provided, different, and unique
        if (userUpdateRequest.getPhone() != null && !userUpdateRequest.getPhone().equals(existingUser.getPhone())) {
            // Check uniqueness
            User userByPhone = query().eq("phone", userUpdateRequest.getPhone()).ne("id", userId).one();
            if (userByPhone != null) {
                 log.warn("Admin update failed for user {}: Phone {} already exists.", userId, userUpdateRequest.getPhone());
                 throw new ConflictException("该手机号已被其他用户使用");
            }
            log.debug("Admin updating phone for user {}", userId);
            existingUser.setPhone(userUpdateRequest.getPhone());
            needsDbUpdate = true;
        }

        // Update email if provided, different, and unique
        if (userUpdateRequest.getEmail() != null && !userUpdateRequest.getEmail().equalsIgnoreCase(existingUser.getEmail())) {
            // Check uniqueness
            User userByEmail = query().eq("email", userUpdateRequest.getEmail()).ne("id", userId).one();
             if (userByEmail != null) {
                 log.warn("Admin update failed for user {}: Email {} already exists.", userId, userUpdateRequest.getEmail());
                 throw new ConflictException("该邮箱已被其他用户使用");
             }
             log.debug("Admin updating email for user {}", userId);
             existingUser.setEmail(userUpdateRequest.getEmail());
             needsDbUpdate = true;
        }

        // Update role if provided and different
        if (userUpdateRequest.getRole() != null && !userUpdateRequest.getRole().equals(existingUser.getRole())) {
            log.debug("Admin updating role for user {}", userId);
            existingUser.setRole(userUpdateRequest.getRole());
            needsDbUpdate = true;
        }

        // Update avatar if provided and different
        if (userUpdateRequest.getAvatar() != null && !userUpdateRequest.getAvatar().equals(existingUser.getAvatar())) {
             log.debug("Admin updating avatar for user {}", userId);
             existingUser.setAvatar(userUpdateRequest.getAvatar());
             needsDbUpdate = true;
        }

        // Perform DB update only if changes were detected
        if (needsDbUpdate) {
            log.info("Admin saving updates for user {}", userId);
            existingUser.setUpdateTime(LocalDateTime.now());
            boolean success = updateById(existingUser); // Use the updated existingUser object
            if (!success) {
                log.error("Admin database update failed unexpectedly for user {}", userId);
                // Throw an exception to trigger rollback and signal failure
                throw new RuntimeException("更新用户信息时发生数据库错误");
            }
            return true; // Update successful
        } else {
            log.warn("Admin update request for user {} resulted in no changes.", userId);
            // No changes needed, but operation is considered successful in the sense that the state is as requested.
            // The controller will return 200 OK with existing data.
            return false; // Return false to indicate no DB rows were affected
        }
    }
} 