package com.example.station.controller;

import com.example.station.annotation.OperationLog;
import com.example.station.common.R;
import com.example.station.dto.ConfigDTO;
import com.example.station.service.SysConfigService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 系统配置接口（仅管理员）
 */
@RestController
@RequestMapping("/config")
@RequiredArgsConstructor
public class ConfigController {

    /** 系统配置业务层 */
    private final SysConfigService sysConfigService;

    /**
     * 查询全部配置（按配置键升序）
     * 请求方式：GET
     * 请求路径：/config/list
     * 返回值：配置列表
     * @return 配置列表
     */
    @GetMapping("/list")
    public R list() {
        return sysConfigService.list();
    }

    /**
     * 新增配置（配置键全局唯一）
     * 请求方式：POST
     * 请求路径：/config/add
     * 请求体：ConfigDTO（configKey、configValue 必填，description/status 选填）
     * 返回值：新增结果
     * @param dto 配置入参
     * @return 新增结果
     */
    @OperationLog(module = "系统配置", operation = "新增配置", description = "配置键：#{#dto.configKey}")
    @PostMapping("/add")
    public R<String> add(@Valid @RequestBody ConfigDTO dto) {
        return sysConfigService.add(dto);
    }

    /**
     * 修改配置（配置键全局唯一查重排除自身；status 不传保持原值）
     * 请求方式：PUT
     * 请求路径：/config/update
     * 请求体：ConfigDTO（id 必填，其余字段同新增）
     * 返回值：修改结果
     * @param dto 配置入参
     * @return 修改结果
     */
    @OperationLog(module = "系统配置", operation = "修改配置", description = "配置键：#{#dto.configKey}")
    @PutMapping("/update")
    public R<String> update(@Valid @RequestBody ConfigDTO dto) {
        return sysConfigService.update(dto);
    }

    /**
     * 删除配置（物理删除，表无逻辑删除列）
     * 请求方式：DELETE
     * 请求路径：/config/delete/{id}
     * 请求参数：路径参数 id 配置ID
     * 返回值：删除结果
     * @param id 配置ID
     * @return 删除结果
     */
    @OperationLog(module = "系统配置", operation = "删除配置", description = "配置ID：#{#id}")
    @DeleteMapping("/delete/{id}")
    public R<String> delete(@PathVariable Long id) {
        return sysConfigService.delete(id);
    }
}
