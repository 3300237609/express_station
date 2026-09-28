package com.example.station.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 快递新增/修改入参（新增与修改共用，修改多要求 id）
 * 取件码系统生成、状态与货位等业务流转字段不在此入参
 */
@Data
public class ExpressDTO {

    private Long id; // 快递ID（修改时必填，由 Service 校验；新增不传）

    @NotBlank(message = "快递单号不能为空")
    @Size(max = 100, message = "快递单号不能超过100位")
    private String expressNo; // 快递单号

    @NotNull(message = "快递公司不能为空")
    private Long companyId; // 快递公司ID

    @Size(max = 50, message = "寄件人不能超过50位")
    private String senderName; // 寄件人（选填）

    @Size(max = 20, message = "寄件人电话不能超过20位")
    private String senderPhone; // 寄件人电话（选填）

    @NotBlank(message = "收件人不能为空")
    @Size(max = 50, message = "收件人不能超过50位")
    private String receiverName; // 收件人

    @NotBlank(message = "收件人电话不能为空")
    @Size(max = 20, message = "收件人电话不能超过20位")
    private String receiverPhone; // 收件人电话

    private Long receiverUserId; // 收件用户ID（选填，须为系统有效用户）

    @Size(max = 20, message = "尺寸类型不能超过20位")
    private String sizeType; // 尺寸类型：SMALL/MEDIUM/LARGE（选填）

    @Min(value = 0, message = "重量不能为负数")
    private BigDecimal weight; // 重量 kg（选填）

    private LocalDateTime arrivalTime; // 到达时间（选填，ISO 格式 2025-01-15T08:00:00）

    @Size(max = 500, message = "备注不能超过500位")
    private String remark; // 备注（选填）
}
