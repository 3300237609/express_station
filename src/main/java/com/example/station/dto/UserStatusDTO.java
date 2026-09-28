package com.example.station.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 用户启用/禁用入参（请求体形态，id 与 status 同时提交）
 */
@Data
public class UserStatusDTO {

    @NotNull(message = "用户ID不能为空")
    private Long id; // 用户ID

    @NotNull(message = "状态不能为空")
    @Min(value = 0, message = "状态值不正确，只能为1正常或0禁用")
    @Max(value = 1, message = "状态值不正确，只能为1正常或0禁用")
    private Integer status; // 账号状态：1 正常，0 禁用
}
