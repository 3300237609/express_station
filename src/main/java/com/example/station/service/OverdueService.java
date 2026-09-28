package com.example.station.service;

import com.example.station.common.R;

/**
 * 超期管理业务接口
 * 超期口径：待取件 + 未标记 + 入库时间早于（当前时间 - station.overdue.hours，缺省 72 小时）
 */
public interface OverdueService {

    /**
     * 分页查询超期快递列表（超期时长按配置时长实时计算，按入库时间升序）
     * @param keyword 关键字（快递单号/收件人/电话模糊），可空
     * @param pageNum 页码，默认第 1 页
     * @param pageSize 每页条数，默认 10 条
     * @return 分页超期快递列表，附 total 总条数
     */
    R list(String keyword, Integer pageNum, Integer pageSize);

    /**
     * 手动触发超期检测（与定时任务共用 checkOverdue，逐条标记并发通知）
     * @return 检测结果（含本次新标记数量）
     */
    R refresh();

    /**
     * 对单个超期快递再次发送提醒（仅已标记超期且关联收件用户的快递）
     * @param id 快递ID
     * @return 提醒结果
     */
    R remind(Long id);

    /**
     * 执行超期检测：将命中口径的快递逐条标记 overdue_flag=1（行级守卫保证只标记/通知一次），
     * 并分别向收件用户与全部管理员发送超期通知
     * @return 本次新标记的快递数量
     */
    int checkOverdue();
}
