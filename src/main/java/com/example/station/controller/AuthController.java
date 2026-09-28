package com.example.station.controller;

import com.example.station.annotation.OperationLog;
import com.example.station.common.R;
import com.example.station.dto.ChangePasswordDTO;
import com.example.station.dto.LoginDTO;
import com.example.station.dto.RegisterDTO;
import com.example.station.service.AuthService;
import com.example.station.vo.LoginVO;
import com.example.station.vo.UserInfoVO;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 认证控制层
 * 处理用户登录、注册、退出、当前用户信息、修改本人密码相关 HTTP 请求
 * 基础路径：/auth
 */
@RestController
@RequestMapping("/auth")
public class AuthController {

    /** 认证业务层 */
    @Autowired
    private AuthService authService;

    /**
     * 用户登录
     * 请求方式：POST
     * 请求路径：/auth/login
     * 请求体：LoginDTO（username 用户名、password 密码）
     * 返回值：Token 与当前用户信息
     */
    @PostMapping("/login")
    public R<LoginVO> login(@Valid @RequestBody LoginDTO dto, HttpServletResponse response) {
        return authService.login(dto, response);
    }

    /**
     * 用户注册（普通用户）
     * 请求方式：POST
     * 请求路径：/auth/register
     * 请求体：RegisterDTO（username、password、confirmPassword、realName、phone、email 选填）
     * 返回值：操作结果
     */
    @OperationLog(module = "用户管理", operation = "用户注册", description = "用户名：#{#dto.username}",
            operator = "#{#dto.username}")
    @PostMapping("/register")
    public R<String> register(@Valid @RequestBody RegisterDTO dto) {
        return authService.register(dto);
    }

    /**
     * 退出登录
     * 请求方式：POST
     * 请求路径：/auth/logout
     * 返回值：操作结果
     */
    @PostMapping("/logout")
    public R<String> logout(HttpServletResponse response) {
        return authService.logout(response);
    }

    /**
     * 获取当前登录用户信息
     * 请求方式：GET
     * 请求路径：/auth/me
     * 请求参数：无（凭登录态）
     * 返回值：当前用户信息（userId、username、realName、phone、email、avatar、roles）
     * @return 当前用户信息
     */
    @GetMapping("/me")
    public R<UserInfoVO> me() {
        return authService.me();
    }

    /**
     * 修改本人密码
     * 请求方式：PUT
     * 请求路径：/auth/changePassword
     * 请求体：ChangePasswordDTO（oldPassword 原密码、newPassword 新密码 6-32 位）
     * 返回值：操作结果
     * @param dto 改密入参
     * @return 操作结果
     */
    @OperationLog(module = "用户管理", operation = "修改密码", description = "修改本人密码")
    @PutMapping("/changePassword")
    public R<String> changePassword(@Valid @RequestBody ChangePasswordDTO dto) {
        return authService.changePassword(dto);
    }
}
