package com.example.station.config;

import com.example.station.common.R;
import com.example.station.security.JwtAuthenticationFilter;
import com.example.station.security.JwtUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.io.IOException;

/**
 * 安全配置类
 * 统一身份认证（Spring Security + JWT）与权限控制（RBAC：ADMIN / COURIER / USER）
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    /** JSON 序列化器，用于认证/授权失败时返回统一响应结构 */
    @Autowired
    private ObjectMapper objectMapper;

    /** JWT 工具类 */
    @Autowired
    private JwtUtils jwtUtils;

    /**
     * 安全过滤器链
     * 无状态 + Token 认证：关闭 CSRF 与 Session，白名单放行，其余接口必须登录
     * @param http HttpSecurity
     * @return 过滤器链
     * @throws Exception 构建异常
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // 1. 关闭 CSRF：前后端分离 + Token 认证，不依赖表单与 Session
                .csrf(csrf -> csrf.disable())
                // 2. 无状态会话：认证信息不存入 Session
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // 2.1 关闭匿名认证：白名单接口的认证信息为 null，未登录语义更清晰
                .anonymous(anon -> anon.disable())
                // 3. 跨域：复用 CorsConfig 中名为 corsConfigurationSource 的 Bean
                .cors(cors -> {})
                // 4. 接口权限：白名单放行；记录视图/超期/趋势统计/日志/配置/公告管理仅 ADMIN，
                //    公司列表/快递列表查询/推荐/入库/异常查询/手机号匹配用户 ADMIN+COURIER（公司列表供入库选公司、快递列表供异常登记换单号、手机号匹配供入库关联收件人），
                //    /user/update 开放登录用户自助改资料（Service 层强制本人并忽略 role），取件/我的消息/异常登记落 anyRequest 登录即可，
                //    我的快递 /express/my 显式登录即可（快递员=本人经手入库视角，管理员/普通用户=收件人视角）
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers("/health", "/auth/login", "/auth/register", "/auth/logout", "/error").permitAll()
                        // 公司列表：快递员入库选公司需要；详情/业务量统计仍仅管理员
                        .requestMatchers(HttpMethod.GET, "/company/list").hasAnyRole("ADMIN", "COURIER")
                        .requestMatchers(HttpMethod.GET, "/company/detail/**", "/company/volume").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/company/add").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/company/update", "/company/updateStatus/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/company/delete/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/user/list", "/user/detail/**",
                                "/courier/list").hasRole("ADMIN")
                        // 按手机号匹配用户：入库选择收件人联动，对快递员开放（精确单条仅回简要字段）
                        .requestMatchers(HttpMethod.GET, "/user/matchByPhone").hasAnyRole("ADMIN", "COURIER")
                        .requestMatchers(HttpMethod.POST, "/user/add", "/courier/add").hasRole("ADMIN")
                        // 个人中心自助改资料：登录即可（Service 层非管理员强制本人、忽略 role，防越权提权）
                        .requestMatchers(HttpMethod.PUT, "/user/update").authenticated()
                        .requestMatchers(HttpMethod.PUT, "/user/updateStatus",
                                "/user/resetPassword/**", "/courier/update").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/user/delete/**", "/courier/delete/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/shelf/list", "/shelf/detail/**",
                                "/shelfLocation/list", "/shelfLocation/detail/**").hasRole("ADMIN")
                        // 空闲货位与智能推荐：入库选位场景对快递员开放
                        .requestMatchers(HttpMethod.GET, "/shelfLocation/free", "/shelfLocation/recommend")
                                .hasAnyRole("ADMIN", "COURIER")
                        .requestMatchers(HttpMethod.POST, "/shelf/add", "/shelfLocation/add").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/shelf/update", "/shelf/updateStatus/**",
                                "/shelfLocation/update", "/shelfLocation/updateStatus/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/shelf/delete/**", "/shelfLocation/delete/**").hasRole("ADMIN")
                        // 快递列表查询：快递员异常登记需按单号换 expressId；详情/流转仍仅管理员
                        .requestMatchers(HttpMethod.GET, "/express/list").hasAnyRole("ADMIN", "COURIER")
                        .requestMatchers(HttpMethod.GET, "/express/detail/**", "/express/flow/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/express/add").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/express/update", "/express/updateStatus/**",
                                "/express/updateLocation").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/express/delete/**").hasRole("ADMIN")
                        // 我的快递：登录即可（显式声明；快递员=本人经手入库视角，管理员/普通用户=收件人视角）
                        .requestMatchers(HttpMethod.GET, "/express/my").authenticated()
                        // 快递入库：管理员与快递员
                        .requestMatchers(HttpMethod.POST, "/inbound/add").hasAnyRole("ADMIN", "COURIER")
                        // 取件记录：管理员与快递员（取件码验证/确认取件落 anyRequest 登录即可）
                        .requestMatchers(HttpMethod.GET, "/pickup/recordList").hasAnyRole("ADMIN", "COURIER")
                        // 异常件：列表/详情 管理员与快递员，处理仅管理员（登记落 anyRequest 登录即可，带归属校验）
                        .requestMatchers(HttpMethod.GET, "/exception/list", "/exception/detail/**")
                                .hasAnyRole("ADMIN", "COURIER")
                        .requestMatchers(HttpMethod.PUT, "/exception/handle").hasRole("ADMIN")
                        // 超期管理：仅管理员（定时任务不受此限制）
                        .requestMatchers(HttpMethod.GET, "/overdue/list").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/overdue/refresh", "/overdue/remind/**").hasRole("ADMIN")
                        // 消息通知：公告发布与通知记录仅管理员（我的消息/已读落 anyRequest）
                        .requestMatchers(HttpMethod.POST, "/notification/publish").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/notification/manage").hasRole("ADMIN")
                        // 统计总览：管理员与快递员（快递员首页展示业务指标）；趋势/日志/系统配置：仅管理员
                        .requestMatchers(HttpMethod.GET, "/stat/dashboard").hasAnyRole("ADMIN", "COURIER")
                        .requestMatchers(HttpMethod.GET, "/stat/trend",
                                "/log/list", "/config/list").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/config/add").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/config/update").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/config/delete/**").hasRole("ADMIN")
                        // 用户/快递员记录视图：仅管理员
                        .requestMatchers(HttpMethod.GET, "/user/express/**", "/user/pickupRecord/**",
                                "/courier/deliveryList/**", "/courier/storageList/**",
                                "/courier/exceptionList/**").hasRole("ADMIN")
                        .anyRequest().authenticated())
                // 5. 注入 JWT 认证过滤器（位于用户名密码认证过滤器之前）
                .addFilterBefore(new JwtAuthenticationFilter(jwtUtils), UsernamePasswordAuthenticationFilter.class)
                // 6. 未认证/越权时不跳转登录页，统一返回 JSON 响应结构
                .exceptionHandling(handler -> handler
                        .authenticationEntryPoint((request, response, authException) ->
                                writeJson(response, HttpServletResponse.SC_UNAUTHORIZED,
                                        "未登录或登录已过期，请重新登录！"))
                        .accessDeniedHandler((request, response, accessDeniedException) ->
                                writeJson(response, HttpServletResponse.SC_FORBIDDEN,
                                        "权限不足，无权访问该资源！")));
        return http.build();
    }

    /**
     * 密码编码器（BCrypt）：注册时加密存储，登录时校验，全程无明文
     * @return 密码编码器
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * 向响应写出 { code, msg, data, map } 结构的 JSON
     * @param response HTTP 响应
     * @param status HTTP 状态码
     * @param msg 错误文案
     * @throws IOException 写出失败
     */
    private void writeJson(HttpServletResponse response, int status, String msg) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(objectMapper.writeValueAsString(R.error(msg)));
    }
}
