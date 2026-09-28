package com.example.station.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 系统公告发布入参（管理员向全部有效用户广播）
 */
@Data
public class NotificationPublishDTO {

    @NotBlank(message = "公告标题不能为空")
    @Size(max = 200, message = "公告标题不能超过200位")
    private String title;       // 公告标题

    @NotBlank(message = "公告内容不能为空")
    @Size(max = 1000, message = "公告内容不能超过1000位")
    private String content;     // 公告内容
}
