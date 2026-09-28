package com.example.station.vo;

import lombok.Data;

/**
 * 快递员个人业绩出参（入库/取件/异常登记 × 今日/本月/累计，固定按当前登录人口径）
 * 入库 = 操作日志「完成入库」且 SUCCESS；取件 = operator_id 为本人的出库记录；异常 = reporter_id 为本人的登记
 */
@Data
public class CourierDashboardVO {

    private Long todayInbound = 0L;   // 今日入库量（本人经手）
    private Long monthInbound = 0L;   // 本月入库量
    private Long totalInbound = 0L;   // 累计入库量
    private Long todayPickup = 0L;    // 今日取件量（本人操作出库）
    private Long monthPickup = 0L;    // 本月取件量
    private Long totalPickup = 0L;    // 累计取件量
    private Long todayException = 0L; // 今日异常登记数（本人登记）
    private Long monthException = 0L; // 本月异常登记数
    private Long totalException = 0L; // 累计异常登记数
}
