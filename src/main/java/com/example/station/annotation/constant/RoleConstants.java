package com.example.station.annotation.constant;

/**
 * 角色常量（与 sys_role 表 role_code 一一对应，SQL 与代码统一引用此来源）
 */
public final class RoleConstants {

    /** 管理员 */
    public static final String ADMIN = "ADMIN";

    /** 快递员 */
    public static final String COURIER = "COURIER";

    /** 普通用户（注册默认分配的角色） */
    public static final String USER = "USER";

    private RoleConstants() {
    }
}
