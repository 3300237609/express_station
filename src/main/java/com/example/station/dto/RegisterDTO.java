package com.example.station.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 用户注册入参（普通用户注册，快递员/管理员由管理员创建）
 */
@Data
public class RegisterDTO {

    @NotBlank(message = "用户名不能为空")
    @Size(min = 3, max = 50, message = "用户名长度须在3-50位之间")
    private String username; // 登录用户名

    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 32, message = "密码长度须在6-32位之间")
    private String password; // 登录密码

    @NotBlank(message = "请再次输入密码")
    private String confirmPassword; // 确认密码

    @NotBlank(message = "姓名不能为空")
    @Size(max = 50, message = "姓名长度不能超过50位")
    private String realName; // 真实姓名

    @NotBlank(message = "手机号不能为空")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone; // 手机号

    @Size(max = 100, message = "邮箱长度不能超过100位")
    @Email(message = "邮箱格式不正确")
    private String email; // 邮箱（选填）
}
