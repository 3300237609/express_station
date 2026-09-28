package com.example.station.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * 货位新增/修改入参（修改时 shelfId 必须与现有一致，所属货架不可迁移）
 */
@Data
public class ShelfLocationDTO {

    private Long id; // 货位ID（修改时必填，由 Service 校验；新增不传）

    @NotNull(message = "所属货架不能为空")
    private Long shelfId; // 所属货架ID

    @NotBlank(message = "货位编号不能为空")
    @Pattern(regexp = "^[A-Za-z0-9\\-]{1,50}$", message = "货位编号只能为字母、数字或横线")
    private String locationNo; // 货位编号（如 A-01-01）

    @NotNull(message = "行号不能为空")
    @Min(value = 1, message = "行号至少为1")
    private Integer rowNo; // 行号

    @NotNull(message = "列号不能为空")
    @Min(value = 1, message = "列号至少为1")
    private Integer columnNo; // 列号

    @NotNull(message = "货位容量不能为空")
    @Min(value = 1, message = "货位容量至少为1")
    private Integer capacity; // 容量（可存放快递件数）
}
