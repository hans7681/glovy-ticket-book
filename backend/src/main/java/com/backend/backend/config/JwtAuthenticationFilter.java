package com.backend.backend.config;

import com.backend.backend.util.JwtUtil;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.SignatureException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private UserDetailsService userDetailsService; // 注入我们上面创建的 Service

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        try {
            String jwt = parseJwt(request);
            if (jwt != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                String username = jwtUtil.extractUsername(jwt);

                UserDetails userDetails = userDetailsService.loadUserByUsername(username);

                if (jwtUtil.validateToken(jwt, userDetails.getUsername())) {
                    // 如果 token 有效，构建 Authentication 对象
                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());

                    // 设置认证信息到 SecurityContext
                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                    logger.debug("用户 '{}' 认证成功，设置 Security Context", username);
                } else {
                     logger.warn("JWT 验证失败: username 不匹配或已过期");
                }
            }
        } catch (ExpiredJwtException e) {
            logger.warn("JWT 已过期: {}", e.getMessage());
        } catch (UnsupportedJwtException e) {
            logger.warn("不支持的 JWT: {}", e.getMessage());
        } catch (MalformedJwtException e) {
            logger.warn("JWT 格式错误: {}", e.getMessage());
        } catch (SignatureException e) {
            logger.warn("JWT 签名无效: {}", e.getMessage());
        } catch (IllegalArgumentException e) {
            logger.warn("JWT claims 字符串为空: {}", e.getMessage());
        } catch (Exception e) {
            logger.error("无法设置用户认证: {}", e.getMessage(), e);
        }

        // 继续执行过滤器链中的下一个过滤器
        filterChain.doFilter(request, response);
    }

    // 从请求头中解析 JWT
    private String parseJwt(HttpServletRequest request) {
        String headerAuth = request.getHeader("Authorization");

        if (StringUtils.hasText(headerAuth) && headerAuth.startsWith("Bearer ")) {
            return headerAuth.substring(7);
        }
        return null;
    }
} 