package com.example.station.service;

import com.example.station.dto.ShelfLocationDTO;
import com.example.station.common.R;
import org.springframework.validation.annotation.Validated;

/**
 * 货位管理业务接口
 */
@Validated
public interface ShelfLocationService {

    /**
     * 分页条件查询货位列表（查看货位状态与容量）
     * @param shelfId 所属货架ID，可空
     * @param status 货位状态：1 空闲，2 部分占用，3 已满，4 停用，可空
     * @param pageNum 页码，默认第 1 页
     * @param pageSize 每页条数，默认 10 条
     * @return 分页货位列表，附 total 总条数
     */
    R list(Long shelfId, Integer status, Integer pageNum, Integer pageSize);

    /**
     * 查询货位详情
     * @param id 货位ID
     * @return 货位详情
     */
    R detail(Long id);

    /**
     * 查询空闲货位（状态=空闲且已用=0，所属货架正常）
     * @param shelfId 所属货架ID，可空（不传查全部货架）
     * @param pageNum 页码，默认第 1 页
     * @param pageSize 每页条数，默认 10 条
     * @return 分页空闲货位列表，附 total 总条数
     */
    R freeList(Long shelfId, Integer pageNum, Integer pageSize);

    /**
     * 新增货位（同步货架总货位数 +1）
     * @param dto 货位入参（货架、编号、行列、容量）
     * @return 新增结果
     */
    R add(@Validated ShelfLocationDTO dto);

    /**
     * 修改货位（所属货架不可迁移；容量不得小于已用）
     * @param dto 货位入参（id 必填）
     * @return 修改结果
     */
    R update(@Validated ShelfLocationDTO dto);

    /**
     * 删除货位（被占用禁止删除，同步货架总货位数 -1）
     * @param id 货位ID
     * @return 删除结果
     */
    R delete(Long id);

    /**
     * 修改货位状态（管理员手动修正：1 空闲，2 部分占用，3 已满，4 停用）
     * @param id 货位ID
     * @param status 目标状态
     * @return 状态修改结果
     */
    R updateStatus(Long id, Integer status);
}
