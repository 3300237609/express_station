package com.example.station.vo;

import lombok.Data;

/**
 * 超期快递出参（含按配置时长计算的超期时长，时间 DATE_FORMAT 转字符串）
 */
@Data
public class OverdueVO {

    private Long id;            // 快递ID
    private String expressNo;   // 快递单号
    private String receiverName;    // 收件人
    private String receiverPhone;   // 收件人电话
    private String pickupCode;  // 取件码
    private Long locationId;    // 货位ID
    private String shelfNo;     // 货架编号
    private String locationNo;  // 货位编号
    private String storageTime; // 入库时间
    private Integer overdueFlag;    // 超期标记：1 已超期
    private Integer overdueHours;    // 超期时长（小时，超过配置时长后的部分）
    private String remark;      // 备注
}
