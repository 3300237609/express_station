package com.example.station.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 货位实体（对应数据库表 shelf_location，无逻辑删除列，删除为物理删除）
 */
@Data
public class ShelfLocation {

    private Long id;             // 主键
    private Long shelfId;        // 所属货架 ID
    private String locationNo;   // 货位编号（如 A-01-01）
    private Integer rowNo;       // 行号
    private Integer columnNo;    // 列号
    private Integer capacity;    // 容量（可存放快递件数）
    private Integer usedCount;   // 已使用数量（由入库流程维护）
    private Integer status;      // 货位状态：1 空闲，2 部分占用，3 已满，4 停用
    private LocalDateTime createTime; // 创建时间
    private LocalDateTime updateTime; // 更新时间
}
