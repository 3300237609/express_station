package com.example.station.service;

import com.example.station.dto.ShelfDTO;
import com.example.station.common.R;
import org.springframework.validation.annotation.Validated;

/**
 * 货架管理业务接口
 */
@Validated
public interface ShelfService {

    /**
     * 分页条件查询货架列表
     * @param keyword 关键字（匹配编号或区域，可空）
     * @param status 货架状态：1 正常，0 停用，可空
     * @param pageNum 页码，默认第 1 页
     * @param pageSize 每页条数，默认 10 条
     * @return 分页货架列表，附 total 总条数
     */
    R list(String keyword, Integer status, Integer pageNum, Integer pageSize);

    /**
     * 查询货架详情
     * @param id 货架ID
     * @return 货架详情（含货位使用率）
     */
    R detail(Long id);

    /**
     * 新增货架
     * @param dto 货架入参（编号、区域、备注）
     * @return 新增结果
     */
    R add(@Validated ShelfDTO dto);

    /**
     * 修改货架基础信息（不改状态与系统计数）
     * @param dto 货架入参（id 必填）
     * @return 修改结果
     */
    R update(@Validated ShelfDTO dto);

    /**
     * 删除货架（名下有占用货位禁止删除，空货位级联删除）
     * @param id 货架ID
     * @return 删除结果
     */
    R delete(Long id);

    /**
     * 启用/停用货架
     * @param id 货架ID
     * @param status 目标状态：1 正常，0 停用
     * @return 启用/停用结果
     */
    R updateStatus(Long id, Integer status);
}
