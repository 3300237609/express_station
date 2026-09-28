package com.example.station.vo;

import lombok.Data;

/**
 * 用户信息出参（列表/详情共用，敏感字段 password 不透出，时间统一 DATE_FORMAT 转字符串）
 */
@Data
public class UserVO {

    private Long id;             // 用户ID
    private String username;     // 登录用户名
    private String realName;     // 真实姓名
    private String phone;        // 手机号
    private String email;        // 邮箱
    private String avatar;       // 头像地址
    private Integer status;      // 账号状态：1 正常，0 禁用
    private String role;         // 角色编码（ADMIN / USER，无角色返回 null）
    private String createTime;   // 创建时间
    private String updateTime;   // 更新时间
}
