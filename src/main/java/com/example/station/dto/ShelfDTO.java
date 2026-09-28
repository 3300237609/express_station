package com.example.station.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 货架新增/修改入参（新增与修改共用，修改多要求 id；capacity/usedCount 系统维护不出现在入参）
 */
@Data
public class ShelfDTO {

    private Long id; // 货架ID（修改时必填，由 Service 校验；新增不传）

    @NotBlank(message = "货架编号不能为空")
    @Pattern(regexp = "^[A-Za-z0-9\\-]{1,50}$", message = "货架编号只能为字母、数字或横线")
    private String shelfNo; // 货架编号（如 A-01）

    @NotBlank(message = "所属区域不能为空")
    @Size(max = 50, message = "所属区域不能超过50位")
    private String area; // 所属区域（如 A 区）

    @Size(max = 255, message = "备注不能超过255位")
    private String remark; // 备注（选填）
}
