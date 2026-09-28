package com.example.station.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;

/**
 * JWT 工具类
 * 负责 Token 的签发与校验：登录成功时签发，请求进入时校验
 */
@Component
public class JwtUtils {

    /** 签名密钥，通过环境变量 JWT_SECRET 注入，不提交明文（长度需 ≥ 32 字节） */
    @Value("${jwt.secret}")
    private String secret;

    /** Token 有效期（天） */
    @Value("${jwt.expire-days:7}")
    private long expireDays;

    /** 存放 Token 的 Cookie 名称（HttpOnly） */
    @Value("${jwt.cookie-name:authToken}")
    private String cookieName;

    /**
     * 生成 JWT（携带角色列表，逗号分隔）
     * @param userId 用户ID
     * @param roles 角色编码列表（如 ADMIN、USER）
     * @return 签发后的 Token
     */
    public String generateToken(String userId, List<String> roles) {
        // 1. 计算签发时间与过期时间
        Date now = new Date();
        Date expireAt = new Date(now.getTime() + expireDays * 24 * 60 * 60 * 1000L);
        // 2. 组装载荷（subject=用户ID，roles=角色列表）并签名返回
        return Jwts.builder()
                .subject(userId)
                .claim("roles", String.join(",", roles))
                .issuedAt(now)
                .expiration(expireAt)
                .signWith(buildKey())
                .compact();
    }

    /**
     * 解析校验 JWT
     * @param token 待解析的 Token
     * @return 校验通过返回载荷；过期、伪造等无效情况返回 null
     */
    public Claims parseToken(String token) {
        if (!StringUtils.hasText(token)) {
            return null;
        }
        try {
            return Jwts.parser()
                    .verifyWith(buildKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (JwtException | IllegalArgumentException e) {
            // Token 无效统一返回 null，由调用方决定处理方式
            return null;
        }
    }

    /**
     * 获取 Cookie 名称
     * @return Cookie 名称（如 authToken）
     */
    public String getCookieName() {
        return cookieName;
    }

    /**
     * 获取 Token 有效期秒数（与 Cookie Max-Age 保持一致）
     * @return 有效期秒数
     */
    public long getExpireSeconds() {
        return expireDays * 24 * 60 * 60;
    }

    /**
     * 构建签名密钥（HS256 要求密钥长度 ≥ 32 字节）
     * @return 密钥
     */
    private SecretKey buildKey() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }
}
