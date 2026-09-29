package com.example.station.controller;

import com.example.station.annotation.OperationLog;
import com.example.station.common.R;
import com.example.station.dto.ExpressDTO;
import com.example.station.dto.UpdateLocationDTO;
import com.example.station.service.ExpressService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

/**
 * 快递信息管理接口
 * 管理员维护快递信息：条件查询、详情、流转记录、新增、修改、删除、状态修正
 */
@RestController
@RequestMapping("/express")
@RequiredArgsConstructor
public class ExpressController {

    /** 快递业务层 */
    private final ExpressService expressService;

    /**
     * 条件分页查询快递列表
     * @param keyword 快递单号关键字，可空
     * @param pickupCode 取件码（模糊），可空
     * @param receiverName 收件人（模糊），可空
     * @param receiverPhone 收件人电话（模糊），可空
     * @param companyId 快递公司ID，可空
     * @param status 快递状态：WAITING/STORED/WAITING_PICKUP/PICKED_UP/EXCEPTION/RETURNED，可空
     * @param arrivalTimeStart 到达时间起（yyyy-MM-dd，含当日），可空
     * @param arrivalTimeEnd 到达时间止（yyyy-MM-dd，含当日），可空
     * @param pageNum 页码，默认第 1 页
     * @param pageSize 每页条数，默认 10 条
     * @return 分页快递列表，附 total 总条数
     */
    @GetMapping("/list")
    public R list(@RequestParam(required = false) String keyword,
                  @RequestParam(required = false) String pickupCode,
                  @RequestParam(required = false) String receiverName,
                  @RequestParam(required = false) String receiverPhone,
                  @RequestParam(required = false) Long companyId,
                  @RequestParam(required = false) String status,
                  @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate arrivalTimeStart,
                  @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate arrivalTimeEnd,
                  @RequestParam(required = false, defaultValue = "1") Integer pageNum,
                  @RequestParam(required = false, defaultValue = "10") Integer pageSize) {
        return expressService.list(keyword, pickupCode, receiverName, receiverPhone,
                companyId, status, arrivalTimeStart, arrivalTimeEnd, pageNum, pageSize);
    }

    /**
     * 查询快递详情
     * @param id 快递ID
     * @return 快递详情（含公司名与货位编号）
     */
    @GetMapping("/detail/{id}")
    public R detail(@PathVariable Long id) {
        return expressService.detail(id);
    }

    /**
     * 按快递单号精确查询（单号全局唯一，货架可视化搜索定位用）
     * 请求方式：GET
     * 请求路径：/express/detailByNo/{expressNo}
     * @param expressNo 快递单号
     * @return 快递详情（含 locationId、locationNo、shelfNo 等）
     */
    @GetMapping("/detailByNo/{expressNo}")
    public R detailByNo(@PathVariable String expressNo) {
        return expressService.detailByExpressNo(expressNo);
    }

    /**
     * 查询快递流转记录
     * @param id 快递ID
     * @return 流转节点列表（创建/到达/入库/异常/取件，按时间正序）
     */
    @GetMapping("/flow/{id}")
    public R flow(@PathVariable Long id) {
        return expressService.flow(id);
    }

    /**
     * 查询我的快递（登录即可；收件用户=我 或 收件手机号=我的手机号，含取件码）
     * 请求方式：GET
     * 请求路径：/express/my
     * 请求参数：keyword（单号，可选）、status（可选）、pageNum（默认1）、pageSize（默认10）
     * 返回值：名下快递分页列表
     * @param keyword 快递单号关键字，可空
     * @param status 快递状态，可空
     * @param pageNum 页码
     * @param pageSize 每页条数
     * @return 分页快递列表，附 total 总条数
     */
    @GetMapping("/my")
    public R my(@RequestParam(required = false) String keyword,
                @RequestParam(required = false) String status,
                @RequestParam(required = false, defaultValue = "1") Integer pageNum,
                @RequestParam(required = false, defaultValue = "10") Integer pageSize) {
        return expressService.myList(keyword, status, pageNum, pageSize);
    }

    /**
     * 新增快递
     * @param dto 快递入参（单号、公司、收件人；取件码系统生成）
     * @return 新增结果
     */
    @OperationLog(module = "快递管理", operation = "新增快递", description = "快递单号：#{#dto.expressNo}")
    @PostMapping("/add")
    public R add(@Valid @RequestBody ExpressDTO dto) {
        return expressService.add(dto);
    }

    /**
     * 修改快递基础信息
     * @param dto 快递入参（id 必填；不改取件码、状态与货位）
     * @return 修改结果
     */
    @OperationLog(module = "快递管理", operation = "修改快递", description = "快递ID：#{#dto.id}，单号：#{#dto.expressNo}")
    @PutMapping("/update")
    public R update(@Valid @RequestBody ExpressDTO dto) {
        return expressService.update(dto);
    }

    /**
     * 删除快递
     * @param id 快递ID（占用货位/有取件记录/有异常登记时禁止删除）
     * @return 删除结果
     */
    @OperationLog(module = "快递管理", operation = "删除快递", description = "快递ID：#{#id}")
    @DeleteMapping("/delete/{id}")
    public R delete(@PathVariable Long id) {
        return expressService.delete(id);
    }

    /**
     * 修改快递状态（管理员修正）
     * @param id 快递ID
     * @param status 目标状态（仍关联货位时禁止置为 PICKED_UP/RETURNED）
     * @return 状态修改结果
     */
    @OperationLog(module = "快递管理", operation = "快递状态修正", description = "快递ID：#{#id}，状态：#{#status}")
    @PutMapping("/updateStatus/{id}")
    public R updateStatus(@PathVariable Long id, @RequestParam String status) {
        return expressService.updateStatus(id, status);
    }

    /**
     * 调整快递货位（管理员：已上架快递换位，占用目标货位、释放原货位并通知收件用户）
     * 请求方式：PUT
     * 请求路径：/express/updateLocation
     * 请求体：UpdateLocationDTO（expressId 快递ID、locationId 目标货位ID 必填）
     * 返回值：调整结果
     * @param dto 调整货位入参
     * @return 调整结果
     */
    @OperationLog(module = "快递管理", operation = "调整货位",
            description = "快递ID：#{#dto.expressId}，目标货位ID：#{#dto.locationId}")
    @PutMapping("/updateLocation")
    public R updateLocation(@Valid @RequestBody UpdateLocationDTO dto) {
        return expressService.updateLocation(dto);
    }
}
