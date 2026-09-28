package com.example.station.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 取件记录实体（对应数据库表 pickup_record，无逻辑删除列）
 * 当前仅用于快递流转记录展示，完整增删改由取件管理模块扩展
 */
@Data
public class PickupRecord {

    private Long id;                // 主键
    private Long expressId;         // 快递 ID（唯一键，一单一条）
    private Long userId;            // 取件人用户 ID
    private String pickupCode;      // 取件码
    private Long operatorId;        // 操作人员 ID
    private String pickupMethod;    // 取件方式：CODE/SCAN/ADMIN/COURIER
    private LocalDateTime pickupTime;   // 取件时间
    private String remark;          // 备注
    private LocalDateTime createTime;   // 创建时间
}
