package com.example.station.vo;

import lombok.Data;

import java.util.List;

/**
 * 当前用户信息出参（敏感字段 password 不透出）
 */
@Data
public class UserInfoVO {

    private Long userId;          // 用户ID
    private String username;      // 用户名
    private String realName;      // 真实姓名
    private String phone;         // 手机号
    private String email;         // 邮箱
    private String avatar;        // 头像地址
    private List<String> roles;   // 角色编码列表（ADMIN / COURIER / USER）
}
