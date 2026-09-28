package com.example.station.vo;

import lombok.Data;

/**
 * 货位信息出参（含所属货架编号与区域，时间统一 DATE_FORMAT 转字符串）
 */
@Data
public class ShelfLocationVO {

    private Long id;             // 货位ID
    private Long shelfId;        // 所属货架ID
    private String shelfNo;      // 货架编号
    private String area;         // 所属区域
    private String locationNo;   // 货位编号
    private Integer rowNo;       // 行号
    private Integer columnNo;    // 列号
    private Integer capacity;    // 容量
    private Integer usedCount;   // 已使用数量
    private Integer status;      // 货位状态：1 空闲，2 部分占用，3 已满，4 停用
    private String createTime;   // 创建时间
    private String updateTime;   // 更新时间
}
