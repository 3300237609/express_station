package com.example.station.entity;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 快递实体（对应数据库表 express，核心业务表）
 * pickup_code 由系统生成（全局唯一，已取件的码永久占号不复用）；status/location_id 等业务流转字段由入库/取件流程维护
 */
@Data
public class Express {

    private Long id;                // 主键
    private String expressNo;       // 快递单号
    private Long companyId;         // 快递公司 ID
    private String senderName;      // 寄件人
    private String senderPhone;     // 寄件人电话
    private String receiverName;    // 收件人
    private String receiverPhone;   // 收件人电话
    private Long receiverUserId;    // 收件用户 ID（选填，关联系统用户）
    private String pickupCode;      // 取件码（系统生成，6 位数字）
    private Long locationId;        // 货位 ID（入库分配，取件释放）
    private String status;          // 快递状态：WAITING/STORED/WAITING_PICKUP/PICKED_UP/EXCEPTION/RETURNED
    private String sizeType;        // 尺寸类型：SMALL/MEDIUM/LARGE
    private BigDecimal weight;      // 重量（kg）
    private LocalDateTime arrivalTime;  // 到达时间
    private LocalDateTime storageTime;  // 入库时间
    private LocalDateTime pickupTime;   // 取件时间
    private Integer overdueFlag;    // 是否超期：0 未超期，1 超期
    private String remark;          // 备注
    private LocalDateTime createTime;   // 创建时间
    private LocalDateTime updateTime;   // 更新时间
    private Integer deleted;        // 逻辑删除：0 未删除，1 已删除
}
