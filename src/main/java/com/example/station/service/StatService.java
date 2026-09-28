package com.example.station.service;

import com.example.station.common.R;
import com.example.station.vo.CourierDashboardVO;

import java.time.LocalDate;

/**
 * 数据统计业务接口
 */
public interface StatService {

    /**
     * 查询工作台统计总览（管理员）
     * @return 统计出参（快递总量/今日入库/今日取件/待取件/异常件/超期件/货位使用率/各公司业务量）
     */
    R dashboard();

    /**
     * 查询入库/取件趋势（按日聚合，区间内无数据的日期补 0）
     * @param dateStart 开始日期（yyyy-MM-dd），可空（缺省结合结束日期回推 7 天）
     * @param dateEnd 结束日期（yyyy-MM-dd），可空（缺省为当天）
     * @return 趋势列表（date、inboundCount、pickupCount）
     */
    R trend(LocalDate dateStart, LocalDate dateEnd);

    /**
     * 查询当前登录人个人业绩（快递员仪表盘展示）
     * @return 业绩出参（入库/取件/异常登记 × 今日/本月/累计，固定本人口径）
     */
    R<CourierDashboardVO> courierDashboard();
}
