package com.example.station.vo;

import lombok.Data;

/**
 * 系统配置出参（时间 DATE_FORMAT 转字符串）
 */
@Data
public class ConfigVO {

    private Long id;            // 配置ID
    private String configKey;   // 配置键
    private String configValue; // 配置值
    private String description; // 配置说明
    private Integer status;     // 状态：1 启用，0 停用
    private String createTime;  // 创建时间
    private String updateTime;  // 更新时间
}
