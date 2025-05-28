package com.backend.backend.config;

import java.util.Arrays;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import static org.springframework.security.config.Customizer.withDefaults;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true, securedEnabled = true) // 启用方法级别的安全注解 (可选)
public class SecurityConfig {

    @Autowired
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    // 如果需要显式注入 AuthenticationManager (例如在 AuthController 中使用)
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // 0. 配置 CORS (允许跨域请求)
            .cors(withDefaults()) // 使用下面的 corsConfigurationSource Bean
            // 1. 禁用 CSRF (因为我们使用 JWT, CSRF 保护不是必需的)
            .csrf(csrf -> csrf
                // 忽略支付宝回调接口的CSRF保护
                .ignoringRequestMatchers("/api/payment/alipay/notify", "/api/payment/alipay/return")
                .disable())
            // 2. 配置 Session 管理策略为无状态 (不创建 Session)
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            // 3. 配置授权规则
            .authorizeHttpRequests(auth -> auth
                // 允许匿名访问的路径 (例如登录、注册、Swagger文档、公共查询接口)
                .requestMatchers("/api/auth/**").permitAll()
                .requestMatchers("/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/movies/**", "/api/cinemas/**", "/api/screenings/**", "/api/announcements").permitAll()
                // 支付宝回调接口（允许匿名访问）
                .requestMatchers("/api/payment/alipay/notify", "/api/payment/alipay/return").permitAll()
                // 管理员接口 (需要 SYSTEM_ADMIN 角色)
                .requestMatchers("/api/admin/**").hasRole("SYSTEM_ADMIN")
                // 影院管理员接口 (需要 CINEMA_ADMIN 角色)
                .requestMatchers("/api/cinema-admin/**").hasRole("CINEMA_ADMIN")
                // 用户特定操作 (例如创建/取消/查看自己的订单，需要已认证)
                .requestMatchers(HttpMethod.POST, "/api/orders").authenticated()
                .requestMatchers(HttpMethod.PUT, "/api/orders/{identifier}/cancel").authenticated()
                .requestMatchers(HttpMethod.GET, "/api/orders/{identifier}").authenticated() // 查看自己的订单 (Service层会进一步校验是否是自己的)
                // 其他所有请求都需要认证
                .anyRequest().authenticated()
            )
            // 4. 将 JWT 过滤器添加到 Spring Security 过滤器链中，在 UsernamePasswordAuthenticationFilter 之前执行
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
            // 5. (可选) 配置更精细的异常处理
            // .exceptionHandling(exceptions -> exceptions
            //     .authenticationEntryPoint(new MyAuthenticationEntryPoint())
            //     .accessDeniedHandler(new MyAccessDeniedHandler())
            // );

        return http.build();
    }

    // 定义 CORS 配置 Bean
    @Bean
    CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        // 允许来自前端开发服务器的请求 (任何 localhost 端口)
        // configuration.setAllowedOrigins(Arrays.asList("http://localhost:5173")); 
        configuration.setAllowedOriginPatterns(Arrays.asList("http://localhost:*")); // 允许 localhost 上的任何端口
        // 允许所有常用的 HTTP 方法
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH")); 
        // 允许所有请求头
        configuration.setAllowedHeaders(Arrays.asList("*")); 
        // 允许携带凭证 (例如 Cookies, Authorization headers)，如果前端需要发送凭证
        configuration.setAllowCredentials(true); 
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        // 对所有路径应用这个 CORS 配置
        source.registerCorsConfiguration("/**", configuration); 
        return source;
    }
} 