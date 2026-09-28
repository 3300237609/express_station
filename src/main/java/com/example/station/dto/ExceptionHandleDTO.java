package com.example.station.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 异常件处理入参（管理员更新处理状态与处理结果）
 */
@Data
public class ExceptionHandleDTO {

    @NotNull(message = "异常记录ID不能为空")
    private Long id;            // 异常记录ID

    @NotBlank(message = "处理状态不能为空")
    @Size(max = 30, message = "处理状态不能超过30位")
    private String handleStatus;    // 处理状态：PENDING/PROCESSING/RESOLVED/CLOSED/REJECTED

    @Size(max = 1000, message = "处理结果不能超过1000位")
    private String handleRemark;    // 处理结果（终态必填，由 Service 校验）
}
