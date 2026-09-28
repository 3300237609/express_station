package com.example.station.service;

import com.example.station.common.R;
import com.example.station.dto.ChangePasswordDTO;
import com.example.station.dto.LoginDTO;
import com.example.station.dto.RegisterDTO;
import com.example.station.vo.LoginVO;
import com.example.station.vo.UserInfoVO;
import jakarta.servlet.http.HttpServletResponse;

/**
 * 认证业务层接口：登录、注册、退出、当前用户信息、修改本人密码
 */
public interface AuthService {

    /**
     * 用户登录：校验账号密码与状态，签发 Token 并写入 HttpOnly Cookie
     * @param dto 登录入参
     * @param response HTTP 响应（用于写入 Token Cookie）
     * @return Token 与当前用户信息
     */
    R<LoginVO> login(LoginDTO dto, HttpServletResponse response);

    /**
     * 用户注册：创建普通用户账号并分配 USER 角色
     * @param dto 注册入参
     * @return 操作结果
     */
    R<String> register(RegisterDTO dto);

    /**
     * 退出登录：清除 Token Cookie
     * @param response HTTP 响应（用于清除 Token Cookie）
     * @return 操作结果
     */
    R<String> logout(HttpServletResponse response);

    /**
     * 获取当前登录用户信息（凭登录态取本人资料与角色列表，敏感字段 password 不透出）
     * @return 当前用户信息
     */
    R<UserInfoVO> me();

    /**
     * 修改本人密码（校验原密码后 BCrypt 加密更新；Token 无状态不作废，由前端引导重新登录）
     * @param dto 改密入参（原密码、新密码）
     * @return 操作结果
     */
    R<String> changePassword(ChangePasswordDTO dto);
}
