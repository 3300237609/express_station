package com.example.station.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 操作日志实体（对应数据库表 operation_log，无逻辑删除列）
 * 由 @OperationLog 注解 + AOP 切面自动写入
 */
@Data
public class OperationLog {

    private Long id;                // 主键
    private Long userId;            // 操作人 ID
    private String username;        // 操作人用户名
    private String roleCode;        // 操作角色
    private String module;          // 操作模块
    private String operation;       // 操作类型
    private String description;     // 操作描述
    private String requestMethod;   // HTTP 方法
    private String requestUrl;      // 请求地址
    private String ipAddress;       // IP 地址
    private String result;          // 操作结果：SUCCESS/FAIL
    private LocalDateTime createTime;   // 操作时间
}
