package com.example.station.controller;

import com.example.station.common.R;
import com.example.station.service.LogService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 操作日志接口（仅管理员；写入由 @OperationLog + AOP 自动完成）
 */
@RestController
@RequestMapping("/log")
@RequiredArgsConstructor
public class LogController {

    /** 操作日志业务层 */
    private final LogService logService;

    /**
     * 分页条件查询操作日志（按操作时间倒序）
     * 请求方式：GET
     * 请求路径：/log/list
     * 请求参数：module、operation、result、keyword（均可选）、pageNum（默认1）、pageSize（默认10）
     * 返回值：日志分页列表
     * @param module 操作模块，可空
     * @param operation 操作类型，可空
     * @param result 操作结果：SUCCESS/FAIL，可空
     * @param keyword 关键字（操作人/描述），可空
     * @param pageNum 页码
     * @param pageSize 每页条数
     * @return 分页日志列表，附 total 总条数
     */
    @GetMapping("/list")
    public R list(@RequestParam(required = false) String module,
                  @RequestParam(required = false) String operation,
                  @RequestParam(required = false) String result,
                  @RequestParam(required = false) String keyword,
                  @RequestParam(required = false, defaultValue = "1") Integer pageNum,
                  @RequestParam(required = false, defaultValue = "10") Integer pageSize) {
        return logService.list(module, operation, result, keyword, null, pageNum, pageSize);
    }
}
