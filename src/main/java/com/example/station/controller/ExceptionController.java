package com.example.station.controller;

import com.example.station.annotation.OperationLog;
import com.example.station.common.R;
import com.example.station.dto.ExceptionDTO;
import com.example.station.dto.ExceptionHandleDTO;
import com.example.station.service.ExceptionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 异常件接口
 * 登记（登录即可，普通用户须归属校验）；查询/处理按角色细分（列表与详情 ADMIN+COURIER，处理仅 ADMIN）
 */
@RestController
@RequestMapping("/exception")
@RequiredArgsConstructor
public class ExceptionController {

    /** 异常件业务层 */
    private final ExceptionService exceptionService;

    /**
     * 分页条件查询异常件列表（管理员/快递员）
     * 请求方式：GET
     * 请求路径：/exception/list
     * 请求参数：expressId、exceptionType、handleStatus、reporterId、keyword（均可选）、pageNum（默认1）、pageSize（默认10）
     * 返回值：异常件分页列表
     * @param expressId 快递ID，可空
     * @param exceptionType 异常类型，可空
     * @param handleStatus 处理状态，可空
     * @param reporterId 登记人ID，可空
     * @param keyword 关键字（单号/收件人），可空
     * @param pageNum 页码
     * @param pageSize 每页条数
     * @return 分页异常件列表，附 total 总条数
     */
    @GetMapping("/list")
    public R list(@RequestParam(required = false) Long expressId,
                  @RequestParam(required = false) String exceptionType,
                  @RequestParam(required = false) String handleStatus,
                  @RequestParam(required = false) Long reporterId,
                  @RequestParam(required = false) String keyword,
                  @RequestParam(required = false, defaultValue = "1") Integer pageNum,
                  @RequestParam(required = false, defaultValue = "10") Integer pageSize) {
        return exceptionService.list(expressId, exceptionType, handleStatus, reporterId, keyword,
                pageNum, pageSize);
    }

    /**
     * 查询异常记录详情（管理员/快递员）
     * 请求方式：GET
     * 请求路径：/exception/detail/{id}
     * 请求参数：路径参数 id 异常记录ID
     * 返回值：异常记录详情
     * @param id 异常记录ID
     * @return 异常记录详情
     */
    @GetMapping("/detail/{id}")
    public R detail(@PathVariable Long id) {
        return exceptionService.detail(id);
    }

    /**
     * 登记异常件（登录即可；普通用户仅可登记收件关联到本人的快递）
     * 请求方式：POST
     * 请求路径：/exception/add
     * 请求体：ExceptionDTO（expressId 与 expressNo 二选一必填、exceptionType、description 必填）
     * 返回值：登记结果
     * @param dto 异常登记入参
     * @return 登记结果
     */
    @OperationLog(module = "异常件管理", operation = "登记异常",
            description = "快递：#{#dto.expressId != null ? #dto.expressId : #dto.expressNo}，类型：#{#dto.exceptionType}")
    @PostMapping("/add")
    public R<String> add(@Valid @RequestBody ExceptionDTO dto) {
        return exceptionService.add(dto);
    }

    /**
     * 处理异常记录（仅管理员；终态须填处理结果并恢复快递状态）
     * 请求方式：PUT
     * 请求路径：/exception/handle
     * 请求体：ExceptionHandleDTO（id、handleStatus 必填，终态 handleRemark 必填）
     * 返回值：处理结果
     * @param dto 异常处理入参
     * @return 处理结果
     */
    @OperationLog(module = "异常件管理", operation = "处理异常",
            description = "记录ID：#{#dto.id}，处理状态：#{#dto.handleStatus}")
    @PutMapping("/handle")
    public R<String> handle(@Valid @RequestBody ExceptionHandleDTO dto) {
        return exceptionService.handle(dto);
    }
}
