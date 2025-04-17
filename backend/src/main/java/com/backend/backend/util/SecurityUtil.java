package com.backend.backend.util;

import com.backend.backend.entity.User;
import com.backend.backend.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

/**
 * 安全工具类，用于获取当前登录用户信息
 */
@Component // 让 Spring 管理，以便注入 UserService
public class SecurityUtil {

    private static UserService staticUserService;

    // 使用 Setter 注入，避免静态字段注入的问题
    @Autowired
    public void setUserService(UserService userService) {
        SecurityUtil.staticUserService = userService;
    }

    /**
     * 获取当前登录的用户实体
     * @return 当前登录的 User 对象，如果未登录或找不到则返回 null 或抛出异常
     */
    public static User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return null; // 或者抛出异常，表示需要认证
        }

        Object principal = authentication.getPrincipal();
        if (principal instanceof UserDetails) {
            UserDetails userDetails = (UserDetails) principal;
            // UserDetails 中通常只包含用户名，需要通过用户名再次查询数据库获取完整 User 对象
            // 注意：这里依赖于 UserDetailsService 返回的 username
            if (staticUserService == null) {
                 // 在测试环境或某些场景下可能未注入
                 throw new IllegalStateException("UserService not injected into SecurityUtil");
            }
            return staticUserService.findByUsername(userDetails.getUsername());
        } else if (principal instanceof String) {
            // 有些情况下 principal 可能只是用户名字符串
             if (staticUserService == null) {
                 throw new IllegalStateException("UserService not injected into SecurityUtil");
             }
            return staticUserService.findByUsername((String) principal);
        }

        // 如果 Principal 不是 UserDetails 或 String，则无法确定用户
        return null;
         // 或者抛出异常 throw new IllegalStateException("无法识别的 Principal 类型");
    }

    /**
     * 获取当前登录用户的 ID
     * @return 用户 ID，如果未登录或找不到则返回 null 或抛出异常
     */
    public static Long getCurrentUserId() {
        User currentUser = getCurrentUser();
        return (currentUser != null) ? currentUser.getId() : null;
    }
} 