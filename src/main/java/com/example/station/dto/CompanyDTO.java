package com.example.station.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 快递公司新增/修改入参（新增与修改共用，修改多要求 id）
 */
@Data
public class CompanyDTO {

    private Long id; // 公司ID（修改时必填，由 Service 校验；新增不传）

    @NotBlank(message = "公司名称不能为空")
    @Size(max = 100, message = "公司名称不能超过100位")
    private String companyName; // 公司名称

    @NotBlank(message = "公司编码不能为空")
    @Pattern(regexp = "^[A-Za-z0-9]{2,50}$", message = "公司编码须为2-50位字母或数字")
    private String companyCode; // 公司编码（如 SF、ZTO）

    @Size(max = 50, message = "联系人不能超过50位")
    private String contactName; // 联系人（选填）

    @Size(max = 20, message = "联系电话不能超过20位")
    private String contactPhone; // 联系电话（选填）
}
