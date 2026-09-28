package com.example.station.service.Impl;

import com.example.station.common.R;
import com.example.station.dto.ConfigDTO;
import com.example.station.entity.SysConfig;
import com.example.station.mapper.SysConfigMapper;
import com.example.station.service.SysConfigService;
import com.example.station.vo.ConfigVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 系统配置业务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SysConfigServiceImpl implements SysConfigService {

    private final SysConfigMapper sysConfigMapper;

    @Override
    public R list() {
        List<ConfigVO> list = sysConfigMapper.selectList();
        return R.success(list);
    }

    @Override
    public R add(ConfigDTO dto) {
        // 1. 兜底空值校验（@Validated 已校验非空，此处保证 Service 单独调用安全）
        if (dto == null || !StringUtils.hasText(dto.getConfigKey())) {
            return R.error("配置键不能为空！");
        }
        if (!StringUtils.hasText(dto.getConfigValue())) {
            return R.error("配置值不能为空！");
        }
        // 2. 配置键全局唯一查重（不过滤状态，与 uk_config_key 同口径）
        String configKey = dto.getConfigKey().trim();
        if (sysConfigMapper.countByKey(configKey, null) > 0) {
            return R.error("配置键已存在！");
        }
        // 3. 落库（状态未传默认启用）
        SysConfig config = new SysConfig();
        config.setConfigKey(configKey);
        config.setConfigValue(dto.getConfigValue().trim());
        config.setDescription(StringUtils.hasText(dto.getDescription()) ? dto.getDescription().trim() : null);
        config.setStatus(dto.getStatus() == null ? 1 : dto.getStatus());
        config.setCreateTime(LocalDateTime.now());
        config.setUpdateTime(LocalDateTime.now());
        sysConfigMapper.insert(config);
        log.info("【配置】新增成功：id={}，configKey={}", config.getId(), configKey);
        return R.success("新增成功！");
    }

    @Override
    public R update(ConfigDTO dto) {
        // 1. 主键必填与兜底空值校验
        if (dto == null || dto.getId() == null) {
            return R.error("配置ID不能为空！");
        }
        if (!StringUtils.hasText(dto.getConfigKey())) {
            return R.error("配置键不能为空！");
        }
        if (!StringUtils.hasText(dto.getConfigValue())) {
            return R.error("配置值不能为空！");
        }
        // 2. 存在性校验（状态未传时保持原值）
        ConfigVO current = sysConfigMapper.selectById(dto.getId());
        if (current == null) {
            return R.error("配置不存在！");
        }
        // 3. 配置键全局唯一查重（排除自身）
        String configKey = dto.getConfigKey().trim();
        if (sysConfigMapper.countByKey(configKey, dto.getId()) > 0) {
            return R.error("配置键已存在！");
        }
        // 4. 全量更新
        SysConfig config = new SysConfig();
        config.setId(dto.getId());
        config.setConfigKey(configKey);
        config.setConfigValue(dto.getConfigValue().trim());
        config.setDescription(StringUtils.hasText(dto.getDescription()) ? dto.getDescription().trim() : null);
        config.setStatus(dto.getStatus() == null ? current.getStatus() : dto.getStatus());
        config.setUpdateTime(LocalDateTime.now());
        sysConfigMapper.update(config);
        log.info("【配置】修改成功：id={}，configKey={}", dto.getId(), configKey);
        return R.success("修改成功！");
    }

    @Override
    public R delete(Long id) {
        // 1. 主键必填与存在性校验
        if (id == null) {
            return R.error("配置ID不能为空！");
        }
        ConfigVO current = sysConfigMapper.selectById(id);
        if (current == null) {
            return R.error("配置不存在！");
        }
        // 2. 物理删除（表无逻辑删除列，删除即释放配置键）
        sysConfigMapper.delete(id);
        log.info("【配置】删除成功：id={}，configKey={}", id, current.getConfigKey());
        return R.success("删除成功！");
    }

    @Override
    public String getValueByKey(String configKey) {
        if (!StringUtils.hasText(configKey)) {
            return null;
        }
        return sysConfigMapper.selectValueByKey(configKey.trim());
    }

    @Override
    public int getIntValue(String configKey, int defaultValue) {
        String value = getValueByKey(configKey);
        if (!StringUtils.hasText(value)) {
            return defaultValue;
        }
        try {
            int parsed = Integer.parseInt(value.trim());
            return parsed > 0 ? parsed : defaultValue;
        } catch (NumberFormatException e) {
            log.warn("【配置】配置值非整数，使用默认值：key={}，value={}，default={}", configKey, value, defaultValue);
            return defaultValue;
        }
    }
}
