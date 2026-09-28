package com.example.station.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 系统配置新增/修改入参（新增与修改共用，修改多要求 id）
 */
@Data
public class ConfigDTO {

    private Long id; // 配置ID（修改时必填，由 Service 校验；新增不传）

    @NotBlank(message = "配置键不能为空")
    @Size(max = 100, message = "配置键不能超过100位")
    private String configKey;   // 配置键（全局唯一）

    @NotBlank(message = "配置值不能为空")
    @Size(max = 500, message = "配置值不能超过500位")
    private String configValue; // 配置值

    @Size(max = 255, message = "配置说明不能超过255位")
    private String description; // 配置说明（选填）

    private Integer status;     // 状态：1 启用，0 停用（选填，新增默认1；修改不传则保持）
}
