package com.example.station.mapper;

import com.example.station.entity.PickupRecord;
import com.example.station.vo.PickupRecordVO;
import com.example.station.vo.PeriodCountVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 取件记录表 PickupRecord 数据访问接口（表无逻辑删除列，uk_express_id 一单一条）
 */
@Mapper
public interface PickupRecordMapper {

    /**
     * 按快递ID查询取件记录（uk_express_id，最多一条）
     * @param expressId 快递ID
     * @return 取件记录列表
     */
    List<PickupRecord> selectByExpressId(@Param("expressId") Long expressId);

    /**
     * 统计快递的取件记录数（取件前置查重，与 uk_express_id 口径一致）
     * @param expressId 快递ID
     * @return 匹配数量
     */
    int countByExpressId(@Param("expressId") Long expressId);

    /**
     * 新增取件记录（回填主键；uk_express_id 冲突抛 DuplicateKeyException 由全局处理器兜底）
     * @param record 取件记录实体
     * @return 影响行数
     */
    int insert(PickupRecord record);

    /**
     * 分页查询我的取件记录（取件人=当前用户，JOIN 快递单号与姓名，按取件时间倒序）
     * @param userId 取件人用户ID
     * @param keyword 快递单号关键字，可空
     * @return 取件记录列表
     */
    List<PickupRecordVO> selectUserRecordList(@Param("userId") Long userId, @Param("keyword") String keyword);

    /**
     * 分页查询取件记录（管理员/快递员视图，JOIN 单号/收件电话/取件人/操作人，按取件时间倒序）
     * @param kw 单号/取件码/用户关键字，可空
     * @param startTime 取件时间起，可空
     * @param endTime 取件时间止，可空
     * @return 取件记录列表
     */
    List<PickupRecordVO> selectRecordList(@Param("kw") String kw,
                                          @Param("startTime") LocalDateTime startTime,
                                          @Param("endTime") LocalDateTime endTime);

    /**
     * 统计本人经手取件数（今日/本月/累计，业绩口径：operator_id=本人，仅管理员/快递员经手时落操作人）
     * @param userId 操作人用户ID
     * @return 三档计数
     */
    PeriodCountVO countPickupByOperator(@Param("userId") Long userId);
}
