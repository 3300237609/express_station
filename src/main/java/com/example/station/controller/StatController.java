package com.example.station.controller;

import com.example.station.common.R;
import com.example.station.service.StatService;
import com.example.station.vo.CourierDashboardVO;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

/**
 * 数据统计接口（统计总览对管理员与快递员开放，趋势仅管理员，个人业绩登录即可按本人口径查询）
 */
@RestController
@RequestMapping("/stat")
@RequiredArgsConstructor
public class StatController {

    /** 数据统计业务层 */
    private final StatService statService;

    /**
     * 查询首页统计总览（总量/今日入库/今日取件/待取/异常/超期/货位使用率/公司业务量）
     * 请求方式：GET
     * 请求路径：/stat/dashboard
     * 返回值：统计总览
     * @return 统计总览
     */
    @GetMapping("/dashboard")
    public R dashboard() {
        return statService.dashboard();
    }

    /**
     * 查询入库/取件趋势（按日聚合，区间内无数据日期补 0）
     * 请求方式：GET
     * 请求路径：/stat/trend
     * 请求参数：dateStart（yyyy-MM-dd，可选，缺省结合 dateEnd 回推近 7 天）、dateEnd（可选，缺省为当天）
     * 返回值：每日趋势列表
     * @param dateStart 开始日期，可空
     * @param dateEnd 结束日期，可空
     * @return 趋势列表
     */
    @GetMapping("/trend")
    public R trend(@RequestParam(required = false)
                   @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate dateStart,
                   @RequestParam(required = false)
                   @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate dateEnd) {
        return statService.trend(dateStart, dateEnd);
    }

    /**
     * 查询当前登录人个人业绩（快递员仪表盘展示用）
     * 请求方式：GET
     * 请求路径：/stat/courierDashboard
     * 请求参数：无（固定按当前登录人口径，登录即可，无法查看他人业绩）
     * 返回值：本人业绩（入库/取件/异常登记 × 今日/本月/累计）
     * @return 本人业绩出参
     */
    @GetMapping("/courierDashboard")
    public R<CourierDashboardVO> courierDashboard() {
        return statService.courierDashboard();
    }
}
