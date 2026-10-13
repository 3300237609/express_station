package com.example.station.service.Impl;

import com.example.station.common.R;
import com.example.station.mapper.ExpressExceptionMapper;
import com.example.station.mapper.ExpressMapper;
import com.example.station.mapper.OperationLogMapper;
import com.example.station.mapper.PickupRecordMapper;
import com.example.station.mapper.ShelfLocationMapper;
import com.example.station.service.CompanyService;
import com.example.station.service.StatCacheService;
import com.example.station.service.StatService;
import com.example.station.utils.SecurityUtils;
import com.example.station.vo.CompanyVolumeVO;
import com.example.station.vo.CourierDashboardVO;
import com.example.station.vo.DashboardVO;
import com.example.station.vo.LocationUsageVO;
import com.example.station.vo.PeriodCountVO;
import com.example.station.vo.TrendVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 数据统计业务实现（只读聚合查询）
 */
@Service
@RequiredArgsConstructor
public class StatServiceImpl implements StatService {

    /** 默认趋势区间（近 7 天） */
    private static final int DEFAULT_TREND_DAYS = 6;

    /** 趋势区间最大跨度（天） */
    private static final int MAX_TREND_DAYS = 365;

    private final ExpressMapper expressMapper;
    private final ExpressExceptionMapper expressExceptionMapper;
    private final ShelfLocationMapper shelfLocationMapper;
    private final OperationLogMapper operationLogMapper;
    private final PickupRecordMapper pickupRecordMapper;
    private final CompanyService companyService;
    private final StatCacheService statCache;

    @Override
    public R dashboard() {
        // 0. 先查缓存（仪表盘允许分钟级延迟，TTL 60 秒）
        DashboardVO cached = statCache.getDashboard();
        if (cached != null) {
            return R.success(cached);
        }
        DashboardVO vo = new DashboardVO();
        // 1. 快递口径统计
        vo.setTotalExpress(zeroIfNull(expressMapper.countAll()));
        vo.setTodayInbound(zeroIfNull(expressMapper.countTodayInbound()));
        vo.setTodayPickup(zeroIfNull(expressMapper.countTodayPickup()));
        vo.setWaitingPickup(zeroIfNull(expressMapper.countWaitingPickup()));
        vo.setOverdueCount(zeroIfNull(expressMapper.countOverdue()));
        // 2. 未处理完的异常记录（待处理 + 处理中）
        vo.setExceptionCount(zeroIfNull(expressExceptionMapper.countOpen()));
        // 3. 货位使用率（正常货架的非停用货位）
        LocationUsageVO usage = shelfLocationMapper.selectUsage();
        long total = usage == null ? 0L : nz(usage.getTotal());
        long used = usage == null ? 0L : nz(usage.getUsed());
        vo.setLocationTotal(total);
        vo.setLocationUsed(used);
        vo.setUsageRate(total > 0 ? String.format("%.2f%%", used * 100.0 / total) : "0.00%");
        // 4. 各公司业务量（复用公司模块统计口径）
        vo.setCompanyVolume(companyVolume());
        statCache.putDashboard(vo);
        return R.success(vo);
    }

    @Override
    public R trend(LocalDate dateStart, LocalDate dateEnd) {
        // 1. 日期缺省：结束日期缺省为当天，开始日期缺省结合结束日期回推近 7 天
        LocalDate end = dateEnd != null ? dateEnd : LocalDate.now();
        LocalDate start = dateStart != null ? dateStart : end.minusDays(DEFAULT_TREND_DAYS);
        // 2. 区间校验
        if (start.isAfter(end)) {
            return R.error("开始日期不能晚于结束日期！");
        }
        if (start.until(end, java.time.temporal.ChronoUnit.DAYS) > MAX_TREND_DAYS) {
            return R.error("时间范围不能超过一年！");
        }
        // 2.5 先查缓存（key 按归一化区间，历史区间数据不变命中率高）
        List<TrendVO> cachedTrend = statCache.getTrend(start, end);
        if (cachedTrend != null) {
            return R.success(cachedTrend);
        }
        // 3. 按日聚合入库与取件量，缺失日期补 0 合并
        Map<String, Long> inboundMap = toMap(expressMapper.selectInboundTrend(start, end), true);
        Map<String, Long> pickupMap = toMap(expressMapper.selectPickupTrend(start, end), false);
        List<TrendVO> list = new ArrayList<>();
        for (LocalDate day = start; !day.isAfter(end); day = day.plusDays(1)) {
            String key = day.toString();
            TrendVO vo = new TrendVO();
            vo.setDate(key);
            vo.setInboundCount(inboundMap.getOrDefault(key, 0L));
            vo.setPickupCount(pickupMap.getOrDefault(key, 0L));
            list.add(vo);
        }
        statCache.putTrend(start, end, list);
        return R.success(list);
    }

    @Override
    public R<CourierDashboardVO> courierDashboard() {
        // 1. 固定当前登录人口径（安全链已保证已认证；接口无入参，天然无法查看他人业绩）
        Long userId = Long.valueOf(SecurityUtils.getCurrentUserId());
        // 1.5 先查缓存（key 按用户，TTL 60 秒）
        CourierDashboardVO cachedCourier = statCache.getCourierDashboard(userId);
        if (cachedCourier != null) {
            return R.success(cachedCourier);
        }
        // 2. 三业务口径聚合：完成入库日志（SUCCESS）/ 本人经手出库记录 / 本人异常登记
        PeriodCountVO inbound = nzp(operationLogMapper.countInboundByUser(userId));
        PeriodCountVO pickup = nzp(pickupRecordMapper.countPickupByOperator(userId));
        PeriodCountVO exception = nzp(expressExceptionMapper.countExceptionByReporter(userId));
        // 3. 组装入库/取件/异常 × 今日/本月/累计三档业绩出参
        CourierDashboardVO vo = new CourierDashboardVO();
        vo.setTodayInbound(inbound.getTodayCount());
        vo.setMonthInbound(inbound.getMonthCount());
        vo.setTotalInbound(inbound.getTotalCount());
        vo.setTodayPickup(pickup.getTodayCount());
        vo.setMonthPickup(pickup.getMonthCount());
        vo.setTotalPickup(pickup.getTotalCount());
        vo.setTodayException(exception.getTodayCount());
        vo.setMonthException(exception.getMonthCount());
        vo.setTotalException(exception.getTotalCount());
        statCache.putCourierDashboard(userId, vo);
        return R.success(vo);
    }

    /**
     * 趋势查询结果转日期索引 Map
     * @param rows 聚合查询结果
     * @param inbound true 索引入库量，false 索引取件量
     * @return 日期 → 数量
     */
    private Map<String, Long> toMap(List<TrendVO> rows, boolean inbound) {
        Map<String, Long> map = new HashMap<>();
        if (rows == null) {
            return map;
        }
        for (TrendVO row : rows) {
            if (row.getDate() == null) {
                continue;
            }
            Long value = inbound ? row.getInboundCount() : row.getPickupCount();
            map.put(row.getDate(), value == null ? 0L : value);
        }
        return map;
    }

    /**
     * 解包公司业务量统计（复用 CompanyService.volume 的权限与口径）
     * @return 业务量列表，统计失败返回空列表
     */
    private List<CompanyVolumeVO> companyVolume() {
        R<List<CompanyVolumeVO>> result = companyService.volume();
        if (result != null && result.getCode() == 1 && result.getData() != null) {
            return result.getData();
        }
        return new ArrayList<>();
    }

    /**
     * Long 计数空值归零
     * @param value 计数值
     * @return 非空值或 0
     */
    private Long zeroIfNull(Long value) {
        return value == null ? 0L : value;
    }

    /**
     * 长整型空值归零
     * @param value 数值
     * @return 非空值或 0
     */
    private long nz(Long value) {
        return value == null ? 0L : value;
    }

    /**
     * 周期计数空行防御（无 GROUP BY 聚合恒返回一行，此处仅兜底归零）
     * @param vo 三档计数行
     * @return 非空三档计数
     */
    private PeriodCountVO nzp(PeriodCountVO vo) {
        return vo == null ? new PeriodCountVO() : vo;
    }
}
