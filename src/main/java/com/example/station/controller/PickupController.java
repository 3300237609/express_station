package com.example.station.controller;

import com.example.station.annotation.OperationLog;
import com.example.station.common.R;
import com.example.station.dto.PickupDTO;
import com.example.station.service.PickupService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

/**
 * 取件接口
 * 取件码验证（只读）→ 确认取件（事务出库）；登录即可，普通用户仅可操作收件关联到本人的快递
 * 取件记录列表仅管理员与快递员
 */
@RestController
@RequestMapping("/pickup")
@RequiredArgsConstructor
public class PickupController {

    /** 取件业务层 */
    private final PickupService pickupService;

    /**
     * 取件码验证（只读，不改变任何状态）
     * 请求方式：POST
     * 请求路径：/pickup/verify
     * 请求参数：code（取件码，必填）
     * 返回值：待取件快递展示信息
     * @param code 取件码
     * @return 快递展示信息
     */
    @PostMapping("/verify")
    public R verify(@RequestParam String code) {
        return pickupService.verify(code);
    }

    /**
     * 确认取件出库（事务：改状态、写取件时间、释放货位、写取件记录、发通知）
     * 请求方式：POST
     * 请求路径：/pickup/confirm
     * 请求体：PickupDTO（code 取件码必填；pickupMethod 选填，普通用户固定 CODE）
     * 返回值：取件结果
     * @param dto 取件入参
     * @return 取件结果
     */
    @OperationLog(module = "取件管理", operation = "确认取件",
            description = "取件码：#{#dto.code}，方式：#{#dto.pickupMethod != null && #dto.pickupMethod != '' ? #dto.pickupMethod : 'CODE'}")
    @PostMapping("/confirm")
    public R<String> confirm(@Valid @RequestBody PickupDTO dto) {
        return pickupService.confirm(dto);
    }

    /**
     * 分页查询取件记录（管理员/快递员，历史取件出库记录查询）
     * 请求方式：GET
     * 请求路径：/pickup/recordList
     * 请求参数：kw（单号/取件码/用户，可选）、startTime/endTime（取件时间范围 yyyy-MM-dd HH:mm:ss，可选）、
     *          pageNum（默认1）、pageSize（默认10）
     * 返回值：取件记录分页列表（含单号/取件码/取件用户/操作人/取件时间）
     * @param kw 单号/取件码/用户关键字，可空
     * @param startTime 取件时间起，可空
     * @param endTime 取件时间止，可空
     * @param pageNum 页码
     * @param pageSize 每页条数
     * @return 分页取件记录列表，附 total 总条数
     */
    @GetMapping("/recordList")
    public R recordList(@RequestParam(required = false) String kw,
                        @RequestParam(required = false)
                        @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startTime,
                        @RequestParam(required = false)
                        @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endTime,
                        @RequestParam(required = false, defaultValue = "1") Integer pageNum,
                        @RequestParam(required = false, defaultValue = "10") Integer pageSize) {
        return pickupService.recordList(kw, startTime, endTime, pageNum, pageSize);
    }
}
