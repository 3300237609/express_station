package com.example.station.mapper;

import com.example.station.entity.OperationLog;
import com.example.station.vo.LogVO;
import com.example.station.vo.PeriodCountVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 操作日志表 OperationLog 数据访问接口（表无逻辑删除列，仅追加写入）
 */
@Mapper
public interface OperationLogMapper {

    /**
     * 新增操作日志（AOP 切面自动写入）
     * @param log 日志实体
     * @return 影响行数
     */
    int insert(OperationLog log);

    /**
     * 条件分页查询操作日志（按操作时间倒序）
     * @param module 操作模块，可空
     * @param operation 操作类型，可空
     * @param result 操作结果：SUCCESS/FAIL，可空
     * @param keyword 关键字（匹配操作人用户名/操作描述），可空
     * @param userId 操作人ID，可空（快递员记录视图按人过滤）
     * @return 日志列表
     */
    List<LogVO> selectList(@Param("module") String module, @Param("operation") String operation,
                           @Param("result") String result, @Param("keyword") String keyword,
                           @Param("userId") Long userId);

    /**
     * 统计本人入库业绩（今日/本月/累计三档）
     * 口径：经本人完成入库且 SUCCESS、快递仍存在的快递，按快递当前入库时间 storage_time 分档，保证个人 ≤ 全站
     * @param userId 操作人用户ID
     * @return 三档计数
     */
    PeriodCountVO countInboundByUser(@Param("userId") Long userId);
}
