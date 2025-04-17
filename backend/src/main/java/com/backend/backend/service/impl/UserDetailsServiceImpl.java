package com.backend.backend.service.impl;

import com.backend.backend.entity.User;
import com.backend.backend.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    @Autowired
    private UserMapper userMapper; // 直接使用 UserMapper 查询

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // 1. 从数据库查询用户信息
        User user = userMapper.findByUsername(username);
        if (user == null) {
            throw new UsernameNotFoundException("用户不存在: " + username);
        }

        // 2. 构建权限列表 (角色需要添加 "ROLE_" 前缀)
        List<GrantedAuthority> authorities = new ArrayList<>();
        if (user.getRole() != null) {
             // Spring Security 默认需要 "ROLE_" 前缀
            authorities.add(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()));
        }

        // 3. 返回 Spring Security 的 UserDetails 对象
        return new org.springframework.security.core.userdetails.User(
                user.getUsername(),
                user.getPassword(), // 数据库中存储的加密密码
                authorities // 用户的权限列表
        );
        // 可以根据需要设置账户是否启用、锁定、过期等，这里简化处理
        // return new org.springframework.security.core.userdetails.User(user.getUsername(), user.getPassword(),
        //        true, true, true, true, authorities);
    }
} 