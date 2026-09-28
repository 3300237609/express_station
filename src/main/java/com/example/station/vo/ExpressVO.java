package com.example.station.vo;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 快递信息出参（关联公司名与货位编号；业务时间 DATE_FORMAT 转字符串）
 */
@Data
public class ExpressVO {

    private Long id;                // 快递ID
    private String expressNo;       // 快递单号
    private Long companyId;         // 快递公司ID
    private String companyName;     // 快递公司名称
    private String senderName;      // 寄件人
    private String senderPhone;     // 寄件人电话
    private String receiverName;    // 收件人
    private String receiverPhone;   // 收件人电话
    private Long receiverUserId;    // 收件用户ID
    private String pickupCode;      // 取件码
    private Long locationId;        // 货位ID
    private String shelfNo;         // 货架编号
    private String locationNo;      // 货位编号
    private String status;          // 快递状态
    private String sizeType;        // 尺寸类型
    private BigDecimal weight;      // 重量 kg
    private String arrivalTime;     // 到达时间
    private String storageTime;     // 入库时间
    private String pickupTime;      // 取件时间
    private Integer overdueFlag;    // 是否超期：0 未超期，1 超期
    private String remark;          // 备注
    private String createTime;      // 创建时间
    private String updateTime;      // 更新时间
}
