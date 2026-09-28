package com.example.station.vo;

import lombok.Data;

/**
 * 货架信息出参（时间统一 DATE_FORMAT 转字符串，使用率由 Service 计算）
 */
@Data
public class ShelfVO {

    private Long id;             // 货架ID
    private String shelfNo;      // 货架编号
    private String area;         // 所属区域
    private Integer capacity;    // 总货位数量
    private Integer usedCount;   // 已使用货位数量
    private Integer status;      // 货架状态：1 正常，0 停用
    private String remark;       // 备注
    private String usageRate;    // 货位使用率（usedCount/capacity，如 30.00%）
    private String createTime;   // 创建时间
    private String updateTime;   // 更新时间
}
