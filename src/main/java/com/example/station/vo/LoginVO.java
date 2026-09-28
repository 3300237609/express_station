package com.example.station.vo;

import lombok.Data;

/**
 * 登录成功出参（Token 同时已写入 HttpOnly Cookie）
 */
@Data
public class LoginVO {

    private String token;        // 登录 Token（兼容 App/调试走 Authorization 头）
    private UserInfoVO userInfo; // 当前用户信息
}
