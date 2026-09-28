package com.example.station.mapper;

import com.example.station.entity.ExpressException;
import com.example.station.vo.ExceptionVO;
import com.example.station.vo.PeriodCountVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 快递异常表 ExpressException 数据访问接口（表无逻辑删除列）
 */
@Mapper
public interface ExpressExceptionMapper {

    /**
     * 按快递ID查询异常记录（按登记时间正序）
     * @param expressId 快递ID
     * @return 异常记录列表
     */
    List<ExpressException> selectByExpressId(@Param("expressId") Long expressId);

    /**
     * 统计未处理完的异常记录数（处理状态=待处理/处理中，统计用）
     * @return 异常件数量
     */
    Long countOpen();

    /**
     * 条件分页查询异常件列表（JOIN 快递单号与登记/处理人姓名，按登记时间倒序）
     * @param expressId 快递ID，可空
     * @param exceptionType 异常类型，可空
     * @param handleStatus 处理状态，可空
     * @param reporterId 登记人ID，可空（快递员异常记录视图按人过滤）
     * @param keyword 单号/收件人关键字，可空
     * @return 异常件列表
     */
    List<ExceptionVO> selectList(@Param("expressId") Long expressId,
                                 @Param("exceptionType") String exceptionType,
                                 @Param("handleStatus") String handleStatus,
                                 @Param("reporterId") Long reporterId,
                                 @Param("keyword") String keyword);

    /**
     * 主键查询异常件详情（JOIN 快递单号与登记/处理人姓名）
     * @param id 异常记录ID
     * @return 异常件详情，不存在返回 null
     */
    ExceptionVO selectById(@Param("id") Long id);

    /**
     * 新增异常登记（回填主键）
     * @param record 异常记录实体
     * @return 影响行数
     */
    int insert(ExpressException record);

    /**
     * 更新异常处理（处理人/处理状态/处理结果/处理时间）
     * @param id 异常记录ID
     * @param handlerId 处理人ID
     * @param handleStatus 处理状态
     * @param handleRemark 处理结果
     * @param handleTime 处理时间
     * @param updateTime 更新时间
     * @return 影响行数
     */
    int updateHandle(@Param("id") Long id, @Param("handlerId") Long handlerId,
                     @Param("handleStatus") String handleStatus, @Param("handleRemark") String handleRemark,
                     @Param("handleTime") LocalDateTime handleTime, @Param("updateTime") LocalDateTime updateTime);

    /**
     * 统计本人异常登记数（今日/本月/累计，业绩口径：reporter_id=本人）
     * @param userId 登记人用户ID
     * @return 三档计数
     */
    PeriodCountVO countExceptionByReporter(@Param("userId") Long userId);
}
