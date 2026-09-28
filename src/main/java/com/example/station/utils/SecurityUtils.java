package com.example.station.utils;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * 当前登录人静态门面
 * 只读取 Spring Security 的 SecurityContextHolder，不持有任何状态、无需清理
 * （SecurityContextHolder 由框架在请求结束时自动清理，禁止再自建用户 ThreadLocal）
 */
public final class SecurityUtils {

    /** 角色权限前缀 */
    private static final String ROLE_PREFIX = "ROLE_";

    private SecurityUtils() {
    }

    /**
     * 获取当前登录用户ID
     * @return 用户ID，未登录返回 null
     */
    public static String getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        // 1. 未认证或匿名认证视为未登录
        if (!isUsable(authentication)) {
            return null;
        }
        // 2. principal 即 JwtAuthenticationFilter 写入的 userId（字符串）
        Object principal = authentication.getPrincipal();
        return principal instanceof String userId ? userId : null;
    }

    /**
     * 获取当前登录用户角色（ADMIN / COURIER / USER）
     * @return 角色标识，未登录返回 null
     */
    public static String getCurrentRole() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        // 1. 未认证或匿名认证视为未登录
        if (!isUsable(authentication)) {
            return null;
        }
        // 2. 从权限集合中取出 ROLE_ 前缀并去掉前缀
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(authority -> authority.startsWith(ROLE_PREFIX))
                .map(authority -> authority.substring(ROLE_PREFIX.length()))
                .findFirst()
                .orElse(null);
    }

    /**
     * 判断当前请求是否已登录
     * @return 已登录返回 true
     */
    public static boolean isAuthenticated() {
        return isUsable(SecurityContextHolder.getContext().getAuthentication());
    }

    /**
     * 判断认证信息是否可用：排除 null、匿名认证与未认证状态
     * @param authentication 认证信息
     * @return 可用返回 true
     */
    private static boolean isUsable(Authentication authentication) {
        return authentication != null
                && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken);
    }
}
