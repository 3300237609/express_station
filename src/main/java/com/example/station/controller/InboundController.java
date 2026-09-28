package com.example.station.controller;

import com.example.station.annotation.OperationLog;
import com.example.station.common.R;
import com.example.station.dto.InboundDTO;
import com.example.station.service.InboundService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 快递入库接口
 * 管理员/快递员凭智能推荐确认的货位完成入库：绑定已录入快递或直接创建并入库
 */
@RestController
@RequestMapping("/inbound")
@RequiredArgsConstructor
public class InboundController {

    /** 入库业务层 */
    private final InboundService inboundService;

    /**
     * 快递入库（事务）
     * 请求方式：POST
     * 请求路径：/inbound/add
     * 请求体：InboundDTO（locationId 必填；expressId 选填=绑定已录入快递，不传=直接创建入库）
     * 返回值：入库后的快递详情（含取件码与货位）
     * @param dto 入库入参
     * @return 入库结果
     */
    @OperationLog(module = "快递入库", operation = "完成入库",
            description = "快递：#{#dto.expressNo != null ? #dto.expressNo : (#dto.expressId != null ? #dto.expressId : '-')}")
    @PostMapping("/add")
    public R add(@RequestBody InboundDTO dto) {
        return inboundService.add(dto);
    }
}
