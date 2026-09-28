package com.example.station.vo;

import lombok.Data;

import java.util.List;

/**
 * 运营统计 Dashboard 出参（快递/取件/异常/超期/货位使用率/公司业务量）
 */
@Data
public class DashboardVO {

    private Long totalExpress;      // 快递总量（未删除）
    private Long todayInbound;      // 今日入库量
    private Long todayPickup;       // 今日取件量
    private Long waitingPickup;     // 待取快递量
    private Long exceptionCount;    // 异常件数量（未处理完的异常记录）
    private Long overdueCount;      // 超期件数量（已标记超期且仍待取）
    private Long locationTotal;     // 货位总容量（正常货架）
    private Long locationUsed;      // 货位已用数量（正常货架）
    private String usageRate;       // 货位使用率（%，保留两位小数）
    private List<CompanyVolumeVO> companyVolume;    // 快递公司业务量
}
