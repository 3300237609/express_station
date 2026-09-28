package com.example.station.vo;

import lombok.Data;

/**
 * 智能货位推荐出参（候选货位 + 评分 + 推荐理由，按评分降序）
 */
@Data
public class LocationRecommendVO {

    private Long shelfId;       // 所属货架ID
    private String shelfNo;     // 货架编号
    private String area;        // 所属区域
    private Long locationId;    // 货位ID
    private String locationNo;  // 货位编号
    private Integer rowNo;      // 行号
    private Integer columnNo;   // 列号
    private Integer capacity;   // 容量
    private Integer usedCount;  // 已使用数量
    private Integer remaining;  // 剩余可用数量
    private Integer status;     // 货位状态：1 空闲，2 部分占用
    private Integer score;      // 推荐评分（满分 100）
    private String reason;      // 推荐理由
}
