package com.example.station.vo;

import lombok.Data;

/**
 * 快递公司信息出参（时间统一 DATE_FORMAT 转字符串）
 */
@Data
public class CompanyVO {

    private Long id;             // 公司ID
    private String companyName;  // 公司名称
    private String companyCode;  // 公司编码
    private String contactName;  // 联系人
    private String contactPhone; // 联系电话
    private Integer status;      // 状态：1 启用，0 停用
    private String createTime;   // 创建时间
    private String updateTime;   // 更新时间
}
