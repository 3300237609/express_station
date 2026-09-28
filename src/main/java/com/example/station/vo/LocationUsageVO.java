package com.example.station.vo;

import lombok.Data;

/**
 * 货位使用汇总出参（统计用：总容量与已用数量）
 */
@Data
public class LocationUsageVO {

    private Long total;     // 总容量（SUM capacity，正常货架）
    private Long used;      // 已用数量（SUM used_count，正常货架）
}
