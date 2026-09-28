package com.example.station.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 异常件登记入参（expressId 与 expressNo 二选一必填）
 */
@Data
public class ExceptionDTO {

    private Long expressId;     // 快递ID（与 expressNo 二选一必填）

    @Size(max = 100, message = "快递单号不能超过100位")
    private String expressNo;   // 快递单号（与 expressId 二选一必填，按单号登记免去换查快递列表的权限依赖）

    @NotBlank(message = "异常类型不能为空")
    @Size(max = 30, message = "异常类型不能超过30位")
    private String exceptionType;   // 异常类型：DAMAGE/LOST/WRONG_ADDR/WRONG_INFO/REFUSED/OVERDUE/OTHER

    @NotBlank(message = "异常描述不能为空")
    @Size(max = 1000, message = "异常描述不能超过1000位")
    private String description;     // 异常描述

    @Size(max = 500, message = "异常图片地址不能超过500位")
    private String imageUrl;        // 异常图片（选填）
}
