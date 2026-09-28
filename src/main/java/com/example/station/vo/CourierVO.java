package com.example.station.vo;

import lombok.Data;

/**
 * 快递员信息出参（列表用，敏感字段 password 不透出，时间统一 DATE_FORMAT 转字符串）
 */
@Data
public class CourierVO {

    private Long id;             // 快递员ID
    private String username;     // 登录用户名
    private String realName;     // 姓名
    private String phone;        // 手机号
    private String email;        // 邮箱
    private Integer status;      // 账号状态：1 正常，0 禁用
    private Long deliveryCount;  // 累计投递数（express 表暂无投递人字段，快递模块落地后接入，当前返回 null 显示 -）
    private String createTime;   // 创建时间
    private String updateTime;   // 更新时间
}
