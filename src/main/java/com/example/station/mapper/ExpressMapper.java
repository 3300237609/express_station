package com.example.station.mapper;

import com.example.station.entity.Express;
import com.example.station.vo.ExpressVO;
import com.example.station.vo.OverdueVO;
import com.example.station.vo.TrendVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 快递表 Express 数据访问接口
 */
@Mapper
public interface ExpressMapper {

    /**
     * 条件分页查询快递列表（关联公司名与货位编号）
     * @param keyword 关键字（匹配快递单号，可空）
     * @param pickupCode 取件码（模糊，可空）
     * @param receiverName 收件人（模糊，可空）
     * @param receiverPhone 收件人电话（模糊，可空）
     * @param companyId 快递公司ID，可空
     * @param status 快递状态，可空
     * @param arrivalTimeStart 到达时间起（含当日00:00，可空）
     * @param arrivalTimeEnd 到达时间止（含当日23:59:59，可空）
     * @return 快递信息列表
     */
    List<ExpressVO> selectList(@Param("keyword") String keyword,
                               @Param("pickupCode") String pickupCode,
                               @Param("receiverName") String receiverName,
                               @Param("receiverPhone") String receiverPhone,
                               @Param("companyId") Long companyId,
                               @Param("status") String status,
                               @Param("arrivalTimeStart") LocalDateTime arrivalTimeStart,
                               @Param("arrivalTimeEnd") LocalDateTime arrivalTimeEnd);

    /**
     * 主键查询快递（逻辑删除排除，关联公司名与货位编号）
     * @param id 快递ID
     * @return 快递信息，不存在返回 null
     */
    ExpressVO selectById(@Param("id") Long id);

    /**
     * 按快递单号查询快递（单号全局唯一；逻辑删除排除，异常登记按单号定位用）
     * @param expressNo 快递单号
     * @return 快递信息，不存在返回 null
     */
    ExpressVO selectByExpressNo(@Param("expressNo") String expressNo);

    /**
     * 按快递单号查重（不过滤 deleted，与 uk_express_no 全局唯一键一致）
     * @param expressNo 快递单号
     * @param excludeId 排除的ID（新增传 null，修改传自身ID）
     * @return 匹配数量
     */
    int countByExpressNo(@Param("expressNo") String expressNo, @Param("excludeId") Long excludeId);

    /**
     * 按取件码查重（不过滤 deleted，与 uk_pickup_code 一致，已取件的码不复用）
     * @param pickupCode 取件码
     * @return 匹配数量
     */
    int countByPickupCode(@Param("pickupCode") String pickupCode);

    /**
     * 新增快递（回填主键）
     * @param express 快递实体
     * @return 影响行数
     */
    int insert(Express express);

    /**
     * 修改快递基础信息（不触碰 pickup_code/status/location_id 与业务时间戳）
     * @param express 快递实体
     * @return 影响行数
     */
    int update(Express express);

    /**
     * 修改快递状态
     * @param id 快递ID
     * @param status 目标状态
     * @param updateTime 更新时间
     * @return 影响行数
     */
    int updateStatus(@Param("id") Long id, @Param("status") String status,
                     @Param("updateTime") LocalDateTime updateTime);

    /**
     * 逻辑删除快递（deleted 置 1；调用方须先校验无货位/取件/异常关联）
     * @param id 快递ID
     * @param updateTime 更新时间
     * @return 影响行数
     */
    int delete(@Param("id") Long id, @Param("updateTime") LocalDateTime updateTime);

    /**
     * 按取件码查询快递（取件码验证用，逻辑删除排除）
     * @param pickupCode 取件码
     * @return 快递信息，不存在返回 null
     */
    ExpressVO selectByPickupCode(@Param("pickupCode") String pickupCode);

    /**
     * 查询名下快递（receiver_user_id 或 receiver_phone 命中，供「我的快递」「用户快递」视图）
     * @param userId 用户ID（必传）
     * @param phone 用户手机号，可空
     * @param keyword 快递单号关键字，可空
     * @param status 快递状态，可空
     * @return 快递列表（按创建时间倒序）
     */
    List<ExpressVO> selectReceiverList(@Param("userId") Long userId, @Param("phone") String phone,
                                       @Param("keyword") String keyword, @Param("status") String status);

    /**
     * 查询本人经手入库的快递（「完成入库」SUCCESS 动作归属，供快递员「我的快递」经手视角）
     * @param userId 快递员用户ID（必传）
     * @param keyword 快递单号关键字，可空
     * @param status 快递状态，可空
     * @return 快递列表（按 id 倒序，逻辑删除排除）
     */
    List<ExpressVO> selectInboundedList(@Param("userId") Long userId, @Param("keyword") String keyword,
                                        @Param("status") String status);

    /**
     * 入库绑定货位（状态守卫：仅待入库 WAITING 且未占货位可绑定；写前校验已通过，affected=0 属并发异常）
     * @param id 快递ID
     * @param locationId 货位ID
     * @param status 目标状态（待取件）
     * @param storageTime 入库时间
     * @param expectStatus 期望的原状态（待入库）
     * @param updateTime 更新时间
     * @return 影响行数
     */
    int updateInbound(@Param("id") Long id, @Param("locationId") Long locationId,
                      @Param("status") String status, @Param("storageTime") LocalDateTime storageTime,
                      @Param("expectStatus") String expectStatus, @Param("updateTime") LocalDateTime updateTime);

    /**
     * 取件出库（状态守卫：仅待取件可取；置已取件、写取件时间、释放货位）
     * @param id 快递ID
     * @param status 目标状态（已取件）
     * @param pickupTime 取件时间
     * @param expectStatus 期望的原状态（待取件）
     * @param updateTime 更新时间
     * @return 影响行数
     */
    int updatePickup(@Param("id") Long id, @Param("status") String status,
                     @Param("pickupTime") LocalDateTime pickupTime,
                     @Param("expectStatus") String expectStatus, @Param("updateTime") LocalDateTime updateTime);

    /**
     * 标记快递超期（overdue_flag 置 1）
     * @param id 快递ID
     * @param updateTime 更新时间
     * @return 影响行数
     */
    int updateOverdueFlag(@Param("id") Long id, @Param("updateTime") LocalDateTime updateTime);

    /**
     * 调整快递货位（货位与状态双守卫防并发出库/重复调整；写前校验已由 Service 完成，affected=0 属并发异常须回滚）
     * @param id 快递ID
     * @param locationId 目标货位ID
     * @param oldLocationId 原货位ID（守卫）
     * @param expectStatus 期望的原状态（守卫）
     * @param updateTime 更新时间
     * @return 影响行数
     */
    int updateLocation(@Param("id") Long id, @Param("locationId") Long locationId,
                       @Param("oldLocationId") Long oldLocationId, @Param("expectStatus") String expectStatus,
                       @Param("updateTime") LocalDateTime updateTime);

    /**
     * 查询超期候选快递（待取件、未标记、入库时间早于截止时间）
     * @param cutoff 截止时间（当前时间减去配置的超期时长）
     * @return 候选快递列表
     */
    List<ExpressVO> selectOverdueCandidates(@Param("cutoff") LocalDateTime cutoff);

    /**
     * 分页查询超期快递列表（已标记超期且仍待取，含按配置时长计算的超期时长）
     * @param keyword 单号/收件人/电话关键字，可空
     * @param hours 超期时长配置（小时）
     * @return 超期快递列表
     */
    List<OverdueVO> selectOverdueList(@Param("keyword") String keyword, @Param("hours") Integer hours);

    /**
     * 统计快递总量（未删除）
     * @return 快递总量
     */
    Long countAll();

    /**
     * 统计今日入库量（入库时间为今天）
     * @return 今日入库量
     */
    Long countTodayInbound();

    /**
     * 统计今日取件量（取件时间为今天）
     * @return 今日取件量
     */
    Long countTodayPickup();

    /**
     * 统计待取快递量（状态=待取件）
     * @return 待取快递量
     */
    Long countWaitingPickup();

    /**
     * 统计超期件数量（已标记超期且仍待取）
     * @return 超期件数量
     */
    Long countOverdue();

    /**
     * 每日入库趋势（按入库日期聚合）
     * @param start 起始日期（含）
     * @param end 结束日期（含）
     * @return 按日期升序的入库趋势
     */
    List<TrendVO> selectInboundTrend(@Param("start") LocalDate start, @Param("end") LocalDate end);

    /**
     * 每日取件趋势（按取件日期聚合）
     * @param start 起始日期（含）
     * @param end 结束日期（含）
     * @return 按日期升序的取件趋势
     */
    List<TrendVO> selectPickupTrend(@Param("start") LocalDate start, @Param("end") LocalDate end);

    /**
     * 按小时聚合的取件量分布（过去 days 天内，按 HOUR(pickup_time) 统计）
     * @param days 天数（过去多少天，必传）
     * @return 每行：hour（0~23）、count；无数据的小时不出现在结果中（调用方补 0）
     */
    List<java.util.Map<String, Object>> selectHourlyPickupDistribution(@Param("days") int days);

    /**
     * 按星期几 + 小时聚合的取件量分布（过去 days 天内）
     * @param days 天数（过去多少天，必传）
     * @return 每行：weekday（1=周一…7=周日）、hour（0~23）、count
     */
    List<java.util.Map<String, Object>> selectWeekdayHourPickupDistribution(@Param("days") int days);
}
