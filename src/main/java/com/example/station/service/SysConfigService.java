package com.example.station.service;

import com.example.station.common.R;
import com.example.station.dto.ConfigDTO;
import org.springframework.validation.annotation.Validated;

/**
 * 系统配置业务接口
 */
@Validated
public interface SysConfigService {

    /**
     * 查询全部配置（按配置键升序）
     * @return 配置列表
     */
    R list();

    /**
     * 新增配置（配置键全局唯一查重，与 uk_config_key 同口径不过滤状态；状态未传默认启用）
     * @param dto 配置入参
     * @return 新增结果
     */
    R add(@Validated ConfigDTO dto);

    /**
     * 修改配置（配置键全局唯一查重排除自身；状态未传保持原值）
     * @param dto 配置入参（id 必填）
     * @return 修改结果
     */
    R update(@Validated ConfigDTO dto);

    /**
     * 删除配置（物理删除，表无逻辑删除列）
     * @param id 配置ID
     * @return 删除结果
     */
    R delete(Long id);

    /**
     * 按配置键读取配置值（业务内部使用，如通知开关、超期时长）
     * @param configKey 配置键
     * @return 配置值，不存在返回 null
     */
    String getValueByKey(String configKey);

    /**
     * 按配置键读取整数配置值（业务内部使用）
     * @param configKey 配置键
     * @param defaultValue 兜底默认值（配置缺失/非法/非正数时返回）
     * @return 整数配置值
     */
    int getIntValue(String configKey, int defaultValue);
}
