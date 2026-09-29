package com.example.station.ai;

import com.example.station.entity.Express;
import com.example.station.mapper.ExpressMapper;
import com.example.station.mapper.ShelfLocationMapper;
import com.example.station.mapper.SysConfigMapper;
import com.example.station.service.SysConfigService;
import com.example.station.vo.LocationUsageVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 取件时间推荐 · Function Calling 工具集
 * <p>
 * 每个被 {@code @Tool} 标记的方法会被 Spring AI 自动注册为 LLM 可调用的函数，
 * LLM 根据 system prompt 和对话上下文自主决定调用哪些工具、按什么顺序调用。
 * 工具之间互相独立，每个只暴露有限的安全查询接口。
 * </p>
 * <p>
 * 返回值统一使用 String（JSON 格式），避免复杂对象的 schema 注册开销；
 * LLM 可以直接消费 JSON 字符串并基于结构化数据做出推荐。
 * </p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PickupRecommendationTools {

    private final ExpressMapper expressMapper;
    private final ShelfLocationMapper shelfLocationMapper;
    private final SysConfigService sysConfigService;

    /** 默认超期阈值（小时），与 OverdueTask / OverdueService 保持一致 */
    private static final int DEFAULT_OVERDUE_HOURS = 48;

    /**
     * 获取当前用户待取快递列表
     *
     * @param userId 用户ID，由调用方（推荐 Service）从 SecurityContext 取出后传入
     * @return JSON 字符串，包含每个待取快递的单号、入库时间、取件码、距超期剩余小时数
     */
    @Tool(description = "获取指定用户当前所有待取快递，包含入库时间、取件码、是否临近超期等信息。推荐取件时间前必须先调用此函数了解用户手上的件量和紧迫度。")
    public String getUserPendingExpress(
            @ToolParam(description = "当前登录用户的ID") Long userId) {
        if (userId == null) {
            return "{\"error\":\"userId 不能为空\"}";
        }
        // 1. 通过 selectReceiverList 拿到 WAITING_PICKUP 状态的快递
        List<com.example.station.vo.ExpressVO> pendingList = expressMapper.selectReceiverList(userId, null, null, "WAITING_PICKUP");
        if (pendingList.isEmpty()) {
            return "{\"count\":0,\"items\":[],\"message\":\"当前用户无待取快递\"}";
        }

        // 2. 组装精简字段：只保留推荐需要的信息
        List<Map<String, Object>> items = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();
        int overdueHours = sysConfigService.getIntValue("station.overdue.hours", DEFAULT_OVERDUE_HOURS);

        for (com.example.station.vo.ExpressVO e : pendingList) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("expressNo", e.getExpressNo());
            item.put("pickupCode", e.getPickupCode());
            item.put("companyName", e.getCompanyName());
            item.put("overdueFlag", e.getOverdueFlag() != null && e.getOverdueFlag() == 1);
            // 计算距超期剩余小时数
            if (e.getStorageTime() != null) {
                LocalDateTime storageTime = LocalDateTime.parse(e.getStorageTime().replace(" ", "T"));
                LocalDateTime deadline = storageTime.plusHours(overdueHours);
                long hoursLeft = java.time.Duration.between(now, deadline).toHours();
                item.put("hoursToOverdue", Math.max(hoursLeft, 0));
                item.put("storageTime", e.getStorageTime());
            } else {
                item.put("hoursToOverdue", null);
            }
            items.add(item);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("count", items.size());
        result.put("overdueThresholdHours", overdueHours);
        result.put("items", items);

        log.debug("【Tool-待取快递】userId={}, count={}", userId, items.size());
        return toJson(result);
    }

    /**
     * 获取驿站各时段取件量分布（整体历史数据）
     *
     * @param days 统计窗口，过去多少天；建议 14~30 天以覆盖足够样本
     * @return JSON 字符串，键为 "hour分布"（长度24）和 "周×小时分布"（按DAYOFWEEK+HOUR）
     */
    @Tool(description = "获取驿站在过去一段时间内各小时的取件量分布，用于判断哪些时段取件人多（高峰期）、哪些时段冷门（低谷期）。同时返回按星期几分别统计的小时分布，帮助区分工作日和周末的不同规律。")
    public String getHourlyPickupDistribution(
            @ToolParam(description = "统计窗口天数，建议 14~30 天") Integer days) {
        int windowDays = (days != null && days > 0) ? days : 14;

        // 1. 整体小时分布（补全 0~23 共 24 个小时的缺失值）
        List<Map<String, Object>> rawHourly = expressMapper.selectHourlyPickupDistribution(windowDays);
        Map<Integer, Long> hourlyMap = new LinkedHashMap<>();
        for (int h = 0; h < 24; h++) {
            hourlyMap.put(h, 0L);
        }
        for (Map<String, Object> row : rawHourly) {
            Number h = (Number) row.get("hour");
            Number c = (Number) row.get("count");
            hourlyMap.put(h.intValue(), c.longValue());
        }

        // 2. 周×小时分布
        List<Map<String, Object>> weekdayHour = expressMapper.selectWeekdayHourPickupDistribution(windowDays);
        List<Map<String, Object>> weekdayHourList = new ArrayList<>();
        for (Map<String, Object> row : weekdayHour) {
            Map<String, Object> wr = new LinkedHashMap<>();
            wr.put("weekday", ((Number) row.get("weekday")).intValue());
            wr.put("hour", ((Number) row.get("hour")).intValue());
            wr.put("count", ((Number) row.get("count")).longValue());
            weekdayHourList.add(wr);
        }

        // 3. 计算全局均值，用于判断高低峰
        long totalPickup = hourlyMap.values().stream().mapToLong(Long::longValue).sum();
        double avgPerHour = totalPickup / 24.0;

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("windowDays", windowDays);
        result.put("totalPickups", totalPickup);
        result.put("avgPickupsPerHour", Math.round(avgPerHour * 100.0) / 100.0);
        result.put("hourDistribution", hourlyMap);
        result.put("weekdayHourDistribution", weekdayHourList);

        log.debug("【Tool-时段分布】windowDays={}, totalPickups={}", windowDays, totalPickup);
        return toJson(result);
    }

    /**
     * 获取驿站超期阈值配置
     *
     * @return JSON 字符串，包含超期判定小时数和系统默认值
     */
    @Tool(description = "获取驿站超期判定的小时阈值配置。超过该时长仍未取件的快递会被标记为超期并通知用户，推荐时需以此为依据判断用户取件的紧迫程度。")
    public String getOverdueThreshold() {
        int configured = sysConfigService.getIntValue("station.overdue.hours", DEFAULT_OVERDUE_HOURS);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("overdueThresholdHours", configured);
        result.put("description", "快递入库后超过此小时数仍未取件即视为超期");
        return toJson(result);
    }

    /**
     * 获取驿站当前货位使用率（货架紧张程度）
     *
     * @return JSON 字符串，包含总容量、已用数量、使用率百分比
     */
    @Tool(description = "获取驿站当前货架货位的整体使用率。货位使用率过高意味着驿站空间紧张、新到快递无处存放或容易出现入库延误，此时应该建议用户尽快取件以缓解压力。")
    public String getStationCapacityStatus() {
        LocationUsageVO usage = shelfLocationMapper.selectUsage();
        long total = usage != null && usage.getTotal() != null ? usage.getTotal() : 0;
        long used = usage != null && usage.getUsed() != null ? usage.getUsed() : 0;
        double rate = total > 0 ? Math.round(used * 10000.0 / total) / 100.0 : 0.0;

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("totalLocations", total);
        result.put("usedLocations", used);
        result.put("usageRatePercent", rate);
        result.put("remainingCapacity", total - used);
        // 定性标签，LLM 可直接消费
        if (rate >= 90) {
            result.put("capacityLevel", "URGENT");
            result.put("capacityHint", "驿站空间紧张，建议尽快取件以避免新到快递无处存放");
        } else if (rate >= 75) {
            result.put("capacityLevel", "TIGHT");
            result.put("capacityHint", "货架使用率较高，近期可能紧张");
        } else if (rate >= 50) {
            result.put("capacityLevel", "NORMAL");
            result.put("capacityHint", "货架使用正常");
        } else {
            result.put("capacityLevel", "AMPLE");
            result.put("capacityHint", "货架充裕");
        }

        log.debug("【Tool-货架状态】total={}, used={}, rate={}%", total, used, rate);
        return toJson(result);
    }

    /**
     * 简单 Map → JSON 字符串（避免引入 Jackson/Gson 的额外依赖，且 Map 键值都是基本类型安全可控）
     */
    private String toJson(Object obj) {
        try {
            return new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(obj);
        } catch (Exception e) {
            // 兜底：toString 不可用时返回空对象
            return "{}";
        }
    }
}
