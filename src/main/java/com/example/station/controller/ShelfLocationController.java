package com.example.station.controller;

import com.example.station.annotation.OperationLog;
import com.example.station.common.R;
import com.example.station.dto.ShelfLocationDTO;
import com.example.station.service.ExpressService;
import com.example.station.service.ShelfLocationService;
import com.example.station.service.ShelfRecommendService;
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

import java.math.BigDecimal;
/**
 * 货位管理接口
 * 管理员维护驿站货位：分页列表、详情、空闲查询、新增、修改、删除、状态修正
 * 货架可视化：grid 全量货位 + pendingLocations 未取件货位
 */
@RestController
@RequestMapping("/shelfLocation")
@RequiredArgsConstructor
public class ShelfLocationController {

    /** 货位业务层 */
    private final ShelfLocationService shelfLocationService;

    /** 智能货位推荐业务层 */
    private final ShelfRecommendService shelfRecommendService;

    /** 快递业务层（货架可视化高亮未取件货位） */
    private final ExpressService expressService;

    /**
     * 分页条件查询货位列表
     * @param shelfId 所属货架ID，可空
     * @param status 货位状态：1 空闲，2 部分占用，3 已满，4 停用，可空
     * @param pageNum 页码，默认第 1 页
     * @param pageSize 每页条数，默认 10 条
     * @return 分页货位列表，附 total 总条数
     */
    @GetMapping("/list")
    public R list(@RequestParam(required = false) Long shelfId,
                  @RequestParam(required = false) Integer status,
                  @RequestParam(required = false, defaultValue = "1") Integer pageNum,
                  @RequestParam(required = false, defaultValue = "10") Integer pageSize) {
        return shelfLocationService.list(shelfId, status, pageNum, pageSize);
    }

    /**
     * 查询货位详情
     * @param id 货位ID
     * @return 货位详情
     */
    @GetMapping("/detail/{id}")
    public R detail(@PathVariable Long id) {
        return shelfLocationService.detail(id);
    }

    /**
     * 查询空闲货位（状态=空闲且已用=0，仅所属货架正常）
     * @param shelfId 所属货架ID，可空（不传查全部货架）
     * @param pageNum 页码，默认第 1 页
     * @param pageSize 每页条数，默认 10 条
     * @return 分页空闲货位列表，附 total 总条数
     */
    @GetMapping("/free")
    public R free(@RequestParam(required = false) Long shelfId,
                  @RequestParam(required = false, defaultValue = "1") Integer pageNum,
                  @RequestParam(required = false, defaultValue = "10") Integer pageSize) {
        return shelfLocationService.freeList(shelfId, pageNum, pageSize);
    }

    /**
     * 智能货位推荐（管理员/快递员入库选位用：按容量匹配+空闲程度+区域匹配打分）
     * 请求方式：GET
     * 请求路径：/shelfLocation/recommend
     * 请求参数：sizeType（SMALL/MEDIUM/LARGE，可选）、weight（kg，可选）、area（区域，如 A 区，可选）
     * 返回值：推荐货位列表（评分降序，附剩余容量与推荐理由）
     * @param sizeType 尺寸类型，可空
     * @param weight 重量 kg，可空（超过 10kg 需求加位）
     * @param area 期望区域，可空
     * @return 推荐货位列表，附评分与推荐理由
     */
    @GetMapping("/recommend")
    public R recommend(@RequestParam(required = false) String sizeType,
                       @RequestParam(required = false) BigDecimal weight,
                       @RequestParam(required = false) String area) {
        return shelfRecommendService.recommend(sizeType, weight, area);
    }

    /**
     * 新增货位
     * @param dto 货位入参（所属货架、编号、行列号、容量）
     * @return 新增结果（同步货架总货位数 +1）
     */
    @OperationLog(module = "货位管理", operation = "新增货位", description = "货位编号：#{#dto.locationNo}")
    @PostMapping("/add")
    public R add(@Valid @RequestBody ShelfLocationDTO dto) {
        return shelfLocationService.add(dto);
    }

    /**
     * 修改货位
     * @param dto 货位入参（id 必填；所属货架不可迁移，容量不得小于已用）
     * @return 修改结果
     */
    @OperationLog(module = "货位管理", operation = "修改货位", description = "货位ID：#{#dto.id}，编号：#{#dto.locationNo}")
    @PutMapping("/update")
    public R update(@Valid @RequestBody ShelfLocationDTO dto) {
        return shelfLocationService.update(dto);
    }

    /**
     * 删除货位
     * @param id 货位ID（被占用禁止删除；同步货架总货位数 -1）
     * @return 删除结果
     */
    @OperationLog(module = "货位管理", operation = "删除货位", description = "货位ID：#{#id}")
    @DeleteMapping("/delete/{id}")
    public R delete(@PathVariable Long id) {
        return shelfLocationService.delete(id);
    }

    /**
     * 修改货位状态（管理员手动修正）
     * @param id 货位ID
     * @param status 目标状态：1 空闲，2 部分占用，3 已满，4 停用
     * @return 状态修改结果
     */
    @OperationLog(module = "货位管理", operation = "货位状态修正", description = "货位ID：#{#id}，状态：#{#status}")
    @PutMapping("/updateStatus/{id}")
    public R updateStatus(@PathVariable Long id, @RequestParam Integer status) {
        return shelfLocationService.updateStatus(id, status);
    }

    // ==================== 货架可视化接口（管理员） ====================

    /**
     * 查询货位（2D 网格渲染用，不分页）
     * 请求路径：GET /shelfLocation/grid?shelfId=1  （可不传 shelfId，查全部货架）
     * @param shelfId 货架ID，可空
     * @return 货位列表（含 shelfId/shelfNo/area/rowNo/columnNo/status/capacity/usedCount）
     */
    @GetMapping("/grid")
    public R grid(@RequestParam(required = false) Long shelfId) {
        return shelfLocationService.gridFull(shelfId);
    }

    /**
     * 查询有未取件快递的货位ID集合（用于前端高亮）
     * 请求路径：GET /shelfLocation/grid/pendingLocations?shelfId=1  （可不传 shelfId，查全部）
     * @param shelfId 货架ID，可空
     * @return 有未取件快递的 location_id 列表（去重）
     */
    @GetMapping("/grid/pendingLocations")
    public R pendingLocations(@RequestParam(required = false) Long shelfId) {
        return expressService.getPendingLocationIdsByShelfId(shelfId);
    }
}
