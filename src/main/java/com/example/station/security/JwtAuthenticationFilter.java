package com.example.station.security;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * JWT 认证过滤器
 * 属于 Spring Security 过滤器链（由 SecurityConfig 注册），负责解析请求携带的 Token：
 * 校验通过则写入安全上下文 SecurityContextHolder（业务代码通过 SecurityUtils 读取）；
 * 无效则不写入，交由认证入口点统一返回 401
 */
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtils jwtUtils;

    public JwtAuthenticationFilter(JwtUtils jwtUtils) {
        this.jwtUtils = jwtUtils;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        // 1. 获取 Token（优先 HttpOnly Cookie，兼容 Authorization 请求头）
        String token = resolveToken(request);

        // 2. 解析校验 Token，通过则写入安全上下文
        if (StringUtils.hasText(token)) {
            Claims claims = jwtUtils.parseToken(token);
            if (claims != null) {
                String userId = claims.getSubject();
                // 1. 读取角色列表（逗号分隔），逐个拼 ROLE_ 前缀权限，支持一个用户多个角色
                List<GrantedAuthority> authorities = new ArrayList<>();
                String roles = claims.get("roles", String.class);
                if (StringUtils.hasText(roles)) {
                    for (String role : roles.split(",")) {
                        if (StringUtils.hasText(role)) {
                            authorities.add(new SimpleGrantedAuthority("ROLE_" + role.trim()));
                        }
                    }
                }
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(userId, null, authorities);
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
            // 3. Token 无效时不写入上下文，后续由认证入口点统一返回 401
        }

        // 4. 放行请求（上下文由 Spring Security 在请求结束的 finally 中自动清理）
        filterChain.doFilter(request, response);
    }

    /**
     * 解析 Token：优先取 Cookie，其次取 Authorization: Bearer 请求头
     * @param request 当前请求
     * @return Token，未携带返回 null
     */
    private String resolveToken(HttpServletRequest request) {
        // 1. HttpOnly Cookie（登录接口写入，path=/，7 天）
        if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if (jwtUtils.getCookieName().equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        // 2. Authorization: Bearer xxx（便于调试及后续 App / 小程序端接入）
        String authorization = request.getHeader("Authorization");
        if (StringUtils.hasText(authorization) && authorization.startsWith("Bearer ")) {
            return authorization.substring(7);
        }
        return null;
    }
}
