package com.example.station.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 快递取件入参（用户/工作人员凭取件码完成取件出库）
 */
@Data
public class PickupDTO {

    @NotBlank(message = "取件码不能为空")
    private String code;            // 取件码（6 位数字）

    private String pickupMethod;    // 取件方式：CODE/SCAN/ADMIN/COURIER（选填，默认 CODE；普通用户强制 CODE）
}
