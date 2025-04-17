package com.backend.backend.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Component
public class JwtUtil {

    @Value("${jwt.secret}")
    private String secretString; // 从配置文件读取密钥字符串

    @Value("${jwt.expiration}")
    private Long expiration;

    private SecretKey secretKey; // 用于签名的 SecretKey 对象

    // 在构造函数或 @PostConstruct 方法中生成 SecretKey
    // 确保 secretString 足够长且安全 (至少 256 位)
    @jakarta.annotation.PostConstruct
    protected void init() {
        // 将 Base64 编码的字符串密钥解码回字节数组
        // byte[] keyBytes = Decoders.BASE64.decode(secretString);
        // 如果 secretString 不是 Base64 编码的, 直接获取字节
        byte[] keyBytes = secretString.getBytes();
        // 使用 keyBytes 创建 SecretKey
        this.secretKey = Keys.hmacShaKeyFor(keyBytes);
    }

    // 从 token 中提取用户名
    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    // 从 token 中提取过期时间
    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    // 从 token 中提取指定的 claim
    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    // 解析 token 获取所有 claims
    private Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(secretKey)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    // 检查 token 是否过期
    private Boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    // 生成 token
    public String generateToken(String username, String role) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("role", role); // 可以添加自定义的 claim，比如用户角色
        return createToken(claims, username);
    }

    // 创建 token 的具体逻辑
    private String createToken(Map<String, Object> claims, String subject) {
        return Jwts.builder()
                .setClaims(claims)
                .setSubject(subject)
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(secretKey, SignatureAlgorithm.HS256)
                .compact();
    }

    // 验证 token 是否有效
    public Boolean validateToken(String token, String username) {
        final String extractedUsername = extractUsername(token);
        return (extractedUsername.equals(username) && !isTokenExpired(token));
    }
} 