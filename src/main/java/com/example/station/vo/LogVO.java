package com.example.station.vo;

import lombok.Data;

/**
 * 操作日志出参（时间 DATE_FORMAT 转字符串）
 */
@Data
public class LogVO {

    private Long id;                // 日志ID
    private Long userId;            // 操作人ID
    private String username;        // 操作人用户名
    private String roleCode;        // 操作角色
    private String module;          // 操作模块
    private String operation;       // 操作类型
    private String description;     // 操作描述
    private String requestMethod;   // HTTP 方法
    private String requestUrl;      // 请求地址
    private String ipAddress;       // IP 地址
    private String result;          // 操作结果：SUCCESS/FAIL
    private String createTime;      // 操作时间
}
