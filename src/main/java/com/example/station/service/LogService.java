package com.example.station.service;

import com.example.station.common.R;

/**
 * 操作日志业务接口（查询侧；写入由 @OperationLog 注解 + AOP 切面自动完成）
 */
public interface LogService {

    /**
     * 条件分页查询操作日志（按操作时间倒序）
     * @param module 操作模块，可空
     * @param operation 操作类型，可空
     * @param result 操作结果：SUCCESS/FAIL，可空
     * @param keyword 关键字（操作人用户名/操作描述），可空
     * @param userId 操作人ID，可空（快递员记录视图按人过滤）
     * @param pageNum 页码，默认第 1 页
     * @param pageSize 每页条数，默认 10 条
     * @return 分页日志列表，附 total 总条数
     */
    R list(String module, String operation, String result, String keyword, Long userId,
           Integer pageNum, Integer pageSize);
}
