package com.example.station.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 货架实体（对应数据库表 shelf）
 * capacity / used_count 由系统维护：增删货位时同步，业务代码不直接赋值
 */
@Data
public class Shelf {

    private Long id;             // 主键
    private String shelfNo;      // 货架编号（如 A-01）
    private String area;         // 所属区域（如 A 区）
    private Integer capacity;    // 总货位数量（= 货位行数，系统维护）
    private Integer usedCount;   // 已使用货位数量（= 占用货位行数，系统维护）
    private Integer status;      // 货架状态：1 正常，0 停用
    private String remark;       // 备注
    private LocalDateTime createTime; // 创建时间
    private LocalDateTime updateTime; // 更新时间
    private Integer deleted;     // 逻辑删除：0 未删除，1 已删除
}
