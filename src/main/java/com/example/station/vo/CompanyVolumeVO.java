package com.example.station.vo;

import lombok.Data;

/**
 * 快递公司业务量统计出参
 */
@Data
public class CompanyVolumeVO {

    private Long companyId;    // 公司ID
    private String companyName; // 公司名称
    private Long totalCount;   // 快递总量（未删除）
}
