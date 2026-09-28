package com.example.station.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 快递公司实体（对应数据库表 express_company）
 */
@Data
public class ExpressCompany {

    private Long id;             // 主键
    private String companyName;  // 公司名称
    private String companyCode;  // 公司编码（如 SF、ZTO）
    private String contactName;  // 联系人
    private String contactPhone; // 联系电话
    private Integer status;      // 状态：1 启用，0 停用
    private LocalDateTime createTime; // 创建时间
    private LocalDateTime updateTime; // 更新时间
    private Integer deleted;     // 逻辑删除：0 未删除，1 已删除
}
