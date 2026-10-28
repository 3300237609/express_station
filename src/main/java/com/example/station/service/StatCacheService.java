package com.example.station.service;

import com.example.station.vo.CourierDashboardVO;
import com.example.station.vo.DashboardVO;
import com.example.station.vo.TrendVO;
import com.fasterxml.jackson.core.type.TypeReference;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

/**
 * 统计数据缓存（计算重、读频繁，短 TTL 保新鲜）。
 * <p>
 * 统计口径随快递写操作持续变化，但仪表盘允许分钟级延迟，
 * 故只靠 60 秒 TTL 刷新，不在各写点挂失效，保持链路简单。
 * </p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StatCacheService {

    private final CommonCacheService cache;

    private static final String DASHBOARD_KEY = "stat:dashboard";
    private static final String COURIER_KEY_PREFIX = "stat:courier:";
    private static final String TREND_KEY_PREFIX = "stat:trend:";

    // ---------- 全局仪表盘 ----------

    public DashboardVO getDashboard() {
        return cache.get(DASHBOARD_KEY, DashboardVO.class);
    }

    public void putDashboard(DashboardVO vo) {
        cache.put(DASHBOARD_KEY, vo, CommonCacheService.TTL_STAT);
    }

    // ---------- 快递员仪表盘（按用户） ----------

    public CourierDashboardVO getCourierDashboard(Long userId) {
        return cache.get(COURIER_KEY_PREFIX + userId, CourierDashboardVO.class);
    }

    public void putCourierDashboard(Long userId, CourierDashboardVO vo) {
        cache.put(COURIER_KEY_PREFIX + userId, vo, CommonCacheService.TTL_STAT);
    }

    // ---------- 入库/取件趋势（按日期区间） ----------

    public List<TrendVO> getTrend(LocalDate start, LocalDate end) {
        return cache.get(trendKey(start, end), new TypeReference<List<TrendVO>>() {
        });
    }

    public void putTrend(LocalDate start, LocalDate end, List<TrendVO> list) {
        cache.put(trendKey(start, end), list, CommonCacheService.TTL_STAT);
    }

    private String trendKey(LocalDate start, LocalDate end) {
        return TREND_KEY_PREFIX + start + "|" + end;
    }
}
