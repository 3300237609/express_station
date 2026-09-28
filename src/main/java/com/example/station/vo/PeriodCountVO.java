package com.example.station.vo;

import lombok.Data;

/**
 * 周期计数出参（今日/本月/累计三档，个人业绩统计各口径共用）
 */
@Data
public class PeriodCountVO {

    private Long todayCount = 0L;   // 今日数量
    private Long monthCount = 0L;   // 本月数量（自然月，每月 1 日起）
    private Long totalCount = 0L;   // 累计数量
}
