package com.example.station.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 系统配置实体（对应数据库表 sys_config，无逻辑删除列，config_key 全局唯一）
 */
@Data
public class SysConfig {

    private Long id;                // 主键
    private String configKey;       // 配置键
    private String configValue;     // 配置值
    private String description;     // 配置说明
    private Integer status;         // 状态：1 启用，0 停用
    private LocalDateTime createTime;   // 创建时间
    private LocalDateTime updateTime;   // 更新时间
}
