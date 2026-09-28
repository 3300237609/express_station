package com.example.station.vo;

import lombok.Data;

/**
 * 取件记录出参（JOIN 快递单号与取件人/操作人姓名，时间 DATE_FORMAT 转字符串）
 */
@Data
public class PickupRecordVO {

    private Long id;                // 取件记录ID
    private Long expressId;         // 快递ID
    private String expressNo;       // 快递单号
    private String receiverName;    // 收件人
    private String receiverPhone;   // 收件人手机号
    private Long userId;            // 取件人用户ID
    private String username;        // 取件人用户名
    private String userName;        // 取件用户姓名（姓名为空回退用户名）
    private Long operatorId;        // 操作人员ID
    private String operatorName;    // 操作人姓名
    private String pickupCode;      // 取件码
    private String pickupMethod;    // 取件方式：CODE/SCAN/ADMIN/COURIER
    private String pickupTime;      // 取件时间
    private String remark;          // 备注
    private String createTime;      // 创建时间
}
