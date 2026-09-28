package com.example.station.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * 跨域配置类
 * 提供名为 corsConfigurationSource 的 Bean，由 Spring Security 过滤器链复用（处理 OPTIONS 预检）
 */
@Configuration
public class CorsConfig {

    /**
     * 跨域规则：开发期允许全部来源；上线前将 allowedOriginPatterns 收敛为前端域名白名单
     * @return 跨域配置源
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        // 允许来源（支持通配模式，配合 allowCredentials 携带 Cookie）
        config.setAllowedOriginPatterns(List.of("*"));
        // 允许的请求方法
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        // 允许的请求头
        config.setAllowedHeaders(List.of("*"));
        // 允许携带 HttpOnly Cookie（authToken）
        config.setAllowCredentials(true);
        // 预检结果缓存 1 小时
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
