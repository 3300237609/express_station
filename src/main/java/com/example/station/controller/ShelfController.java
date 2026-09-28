package com.example.station.controller;

import com.example.station.annotation.OperationLog;
import com.example.station.common.R;
import com.example.station.dto.ShelfDTO;
import com.example.station.service.ShelfService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 货架管理接口
 * 管理员维护驿站货架：分页列表、详情、新增、修改、删除、启用/停用
 */
@RestController
@RequestMapping("/shelf")
@RequiredArgsConstructor
public class ShelfController {

    /** 货架业务层 */
    private final ShelfService shelfService;

    /**
     * 分页条件查询货架列表
     * @param keyword 关键字（匹配编号或区域，可空）
     * @param status 货架状态：1 正常，0 停用，可空
     * @param pageNum 页码，默认第 1 页
     * @param pageSize 每页条数，默认 10 条
     * @return 分页货架列表，附 total 总条数
     */
    @GetMapping("/list")
    public R list(@RequestParam(required = false) String keyword,
                  @RequestParam(required = false) Integer status,
                  @RequestParam(required = false, defaultValue = "1") Integer pageNum,
                  @RequestParam(required = false, defaultValue = "10") Integer pageSize) {
        return shelfService.list(keyword, status, pageNum, pageSize);
    }

    /**
     * 查询货架详情
     * @param id 货架ID
     * @return 货架详情（含货位使用率）
     */
    @GetMapping("/detail/{id}")
    public R detail(@PathVariable Long id) {
        return shelfService.detail(id);
    }

    /**
     * 新增货架
     * @param dto 货架入参（编号、区域、备注）
     * @return 新增结果
     */
    @OperationLog(module = "货架管理", operation = "新增货架", description = "货架编号：#{#dto.shelfNo}")
    @PostMapping("/add")
    public R add(@Valid @RequestBody ShelfDTO dto) {
        return shelfService.add(dto);
    }

    /**
     * 修改货架基础信息
     * @param dto 货架入参（id 必填；不改状态与系统计数）
     * @return 修改结果
     */
    @OperationLog(module = "货架管理", operation = "修改货架", description = "货架ID：#{#dto.id}，编号：#{#dto.shelfNo}")
    @PutMapping("/update")
    public R update(@Valid @RequestBody ShelfDTO dto) {
        return shelfService.update(dto);
    }

    /**
     * 删除货架
     * @param id 货架ID（名下有占用货位禁止删除，空货位级联删除）
     * @return 删除结果
     */
    @OperationLog(module = "货架管理", operation = "删除货架", description = "货架ID：#{#id}")
    @DeleteMapping("/delete/{id}")
    public R delete(@PathVariable Long id) {
        return shelfService.delete(id);
    }

    /**
     * 启用/停用货架
     * @param id 货架ID
     * @param status 目标状态：1 正常，0 停用
     * @return 启用/停用结果
     */
    @OperationLog(module = "货架管理", operation = "启停货架", description = "货架ID：#{#id}，状态：#{#status}")
    @PutMapping("/updateStatus/{id}")
    public R updateStatus(@PathVariable Long id, @RequestParam Integer status) {
        return shelfService.updateStatus(id, status);
    }
}
