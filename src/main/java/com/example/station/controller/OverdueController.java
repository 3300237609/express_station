package com.example.station.controller;

import com.example.station.annotation.OperationLog;
import com.example.station.common.R;
import com.example.station.service.OverdueService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 超期管理接口（仅管理员；每小时定时检测与手动触发共用同一实现）
 */
@RestController
@RequestMapping("/overdue")
@RequiredArgsConstructor
public class OverdueController {

    /** 超期管理业务层 */
    private final OverdueService overdueService;

    /**
     * 分页查询超期快递列表（超期时长按 station.overdue.hours 配置实时计算）
     * 请求方式：GET
     * 请求路径：/overdue/list
     * 请求参数：keyword（单号/收件人/电话，可选）、pageNum（默认1）、pageSize（默认10）
     * 返回值：超期快递分页列表
     * @param keyword 关键字，可空
     * @param pageNum 页码
     * @param pageSize 每页条数
     * @return 分页超期快递列表，附 total 总条数
     */
    @GetMapping("/list")
    public R list(@RequestParam(required = false) String keyword,
                  @RequestParam(required = false, defaultValue = "1") Integer pageNum,
                  @RequestParam(required = false, defaultValue = "10") Integer pageSize) {
        return overdueService.list(keyword, pageNum, pageSize);
    }

    /**
     * 手动触发超期检测（与定时任务共用实现）
     * 请求方式：POST
     * 请求路径：/overdue/refresh
     * 返回值：检测结果（含本次新标记数量）
     * @return 检测结果
     */
    @OperationLog(module = "超期管理", operation = "超期检测")
    @PostMapping("/refresh")
    public R<String> refresh() {
        return overdueService.refresh();
    }

    /**
     * 对单个超期快递再次发送提醒
     * 请求方式：POST
     * 请求路径：/overdue/remind/{id}
     * 请求参数：路径参数 id 快递ID
     * 返回值：提醒结果
     * @param id 快递ID
     * @return 提醒结果
     */
    @OperationLog(module = "超期管理", operation = "超期提醒", description = "快递ID：#{#id}")
    @PostMapping("/remind/{id}")
    public R<String> remind(@PathVariable Long id) {
        return overdueService.remind(id);
    }
}
