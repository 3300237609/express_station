package com.example.station.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 快递员新增/修改入参（新增与修改共用，修改多要求 id）
 * 角色由后端固定分配 COURIER，入参不接收角色
 * 密码仅新增时必填，由 Service 校验（@Valid 无法区分新增/修改场景，故不加 @NotBlank）
 */
@Data
public class CourierDTO {

    private Long id; // 快递员ID（修改时必填，由 Service 校验；新增不传）

    @NotBlank(message = "用户名不能为空")
    @Size(min = 3, max = 50, message = "用户名长度须在3-50位之间")
    private String username; // 登录用户名

    @Size(min = 6, max = 32, message = "密码长度须在6-32位之间")
    private String password; // 初始密码（新增必填，修改不传）

    @NotBlank(message = "姓名不能为空")
    @Size(max = 50, message = "姓名长度不能超过50位")
    private String realName; // 姓名

    @NotBlank(message = "手机号不能为空")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone; // 手机号

    @Size(max = 100, message = "邮箱长度不能超过100位")
    @Email(message = "邮箱格式不正确")
    private String email; // 邮箱（选填）
}
