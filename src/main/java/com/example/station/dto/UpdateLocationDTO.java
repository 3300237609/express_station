package com.example.station.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 调整快递货位入参
 */
@Data
public class UpdateLocationDTO {

    @NotNull(message = "快递ID不能为空")
    private Long expressId;      // 快递ID

    @NotNull(message = "目标货位不能为空")
    private Long locationId;     // 目标货位ID
}
