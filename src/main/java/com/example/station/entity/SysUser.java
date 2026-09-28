package com.example.station.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户实体（对应数据库表 sys_user）
 */
@Data
public class SysUser {

    private Long id;             // 主键
    private String username;     // 登录用户名
    private String password;     // 加密后的密码（BCrypt，禁止出任何响应）
    private String realName;     // 真实姓名
    private String phone;        // 手机号
    private String email;        // 邮箱
    private String avatar;       // 头像地址
    private Integer status;      // 账号状态：1 正常，0 禁用
    private LocalDateTime createTime; // 创建时间
    private LocalDateTime updateTime; // 更新时间
    private Integer deleted;     // 逻辑删除：0 未删除，1 已删除
}
