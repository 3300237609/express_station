package com.example.station.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 快递异常记录实体（对应数据库表 express_exception）
 * 当前仅用于快递流转记录展示，完整登记/处理由异常件管理模块扩展
 */
@Data
public class ExpressException {

    private Long id;                // 主键
    private Long expressId;         // 快递 ID
    private String exceptionType;   // 异常类型：DAMAGE/LOST/WRONG_ADDR/WRONG_INFO/REFUSED/OVERDUE/OTHER
    private String description;     // 异常描述
    private String imageUrl;        // 异常图片
    private Long reporterId;        // 登记人
    private Long handlerId;         // 处理人
    private String handleStatus;    // 处理状态：PENDING/PROCESSING/RESOLVED/CLOSED/REJECTED
    private String handleRemark;    // 处理结果
    private LocalDateTime createTime;   // 登记时间
    private LocalDateTime handleTime;   // 处理时间
    private LocalDateTime updateTime;   // 更新时间
}
