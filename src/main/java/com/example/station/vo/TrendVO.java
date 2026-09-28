package com.example.station.vo;

import lombok.Data;

/**
 * 每日趋势出参（入库/取件按日聚合，日期 YYYY-MM-DD）
 */
@Data
public class TrendVO {

    private String date;            // 日期（YYYY-MM-DD）
    private Long inboundCount;      // 当日入库量
    private Long pickupCount;       // 当日取件量
}
