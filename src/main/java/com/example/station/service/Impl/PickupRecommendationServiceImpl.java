package com.example.station.service.Impl;

import com.example.station.ai.PickupRecommendationTools;
import com.example.station.common.R;
import com.example.station.mapper.ExpressMapper;
import com.example.station.mapper.ShelfLocationMapper;
import com.example.station.service.PickupRecommendationService;
import com.example.station.service.SysConfigService;
import com.example.station.utils.SecurityUtils;
import com.example.station.vo.ExpressVO;
import com.example.station.vo.LocationUsageVO;
import com.example.station.vo.PickupRecommendationVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 取件时间推荐业务实现
 * <p>
 * 双模式：
 * <ol>
 *   <li><b>LLM Function Calling 模式</b>：ChatClient + 4 个 Tool Bean 自动装配，
 *       LLM 自主决定调用哪些工具收集数据，然后生成推荐。
 *       通过 {@code station.pickup-recommend.llm-enabled=true} 启用；API Key 为空时自动跳过。</li>
 *   <li><b>规则引擎降级模式</b>：LLM 不可用时直接在 Service 层用 Java 代码计算——
 *       先看用户待取快递量和紧迫度，再看驿站小时分布找低谷时段，按规则组合输出推荐。</li>
 * </ol>
 * 两种模式共用同一套返回 VO（PickupRecommendationVO），前端无感知。
 * </p>
 */
@Slf4j
@Service
public class PickupRecommendationServiceImpl implements PickupRecommendationService {

    /** MyBatis Mapper 等核心依赖——构造器注入（Spring 推荐方式） */
    private final ExpressMapper expressMapper;
    private final ShelfLocationMapper shelfLocationMapper;
    private final SysConfigService sysConfigService;
    private final PickupRecommendationTools recommendationTools;

    /** ChatClient Builder —— 可选注入（Spring AI 自动配置在无 API Key 时不创建此 bean） */
    private final ChatClient.Builder chatClientBuilder;

    /**
     * 构造器：ChatClient.Builder 通过 ObjectProvider 包装为可选，
     * 如果 Spring AI 自动配置没生效（API Key 缺失或排除自动配置），这里拿到 null 就降级规则引擎。
     */
    public PickupRecommendationServiceImpl(
            ExpressMapper expressMapper,
            ShelfLocationMapper shelfLocationMapper,
            SysConfigService sysConfigService,
            PickupRecommendationTools recommendationTools,
            ObjectProvider<ChatClient.Builder> chatClientBuilderProvider) {
        this.expressMapper = expressMapper;
        this.shelfLocationMapper = shelfLocationMapper;
        this.sysConfigService = sysConfigService;
        this.recommendationTools = recommendationTools;
        // 如果 bean 不存在，ObjectProvider.getIfAvailable() 返回 null
        this.chatClientBuilder = chatClientBuilderProvider.getIfAvailable();
    }

    /** LLM 模型是否启用（配置开关） */
    @Value("${station.pickup-recommend.llm-enabled:true}")
    private boolean llmEnabled;

    /** LLM API Key（空字符串时视为未配置，直接降级） */
    @Value("${spring.ai.openai.api-key:}")
    private String llmApiKey;

    /** 默认超期阈值 */
    private static final int DEFAULT_OVERDUE_HOURS = 48;

    /** 时段格式化器 */
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    /** 驿站营业时间（取件服务时间范围）——可做成系统配置 */
    private static final int OPEN_HOUR = 8;
    private static final int CLOSE_HOUR = 22;

    // ---------------- 入口 ----------------

    @Override
    public R recommend() {
        // 1. 获取当前登录人（SecurityUtils 是纯静态门面，返回 String userId）
        String userIdStr = SecurityUtils.getCurrentUserId();
        Long userId;
        try {
            userId = userIdStr != null ? Long.parseLong(userIdStr) : null;
        } catch (NumberFormatException e) {
            log.warn("【取件推荐】userId 格式异常：{}", userIdStr);
            return R.error("登录信息异常，请重新登录！");
        }
        if (userId == null) {
            return R.error("请先登录！");
        }

        // 2. 先准备降级路径所需数据（无论走 LLM 还是规则引擎都需要）
        ContextData ctx = buildContextData(userId);

        // 3. 判断是否走 LLM：开关开 + API Key 不为空 + ChatClient bean 存在（自动配置成功）
        if (llmEnabled && StringUtils.hasText(llmApiKey) && chatClientBuilder != null) {
            try {
                PickupRecommendationVO result = callLlm(userId, ctx);
                if (result != null) {
                    log.info("【取件推荐】LLM 模式成功，userId={}", userId);
                    return R.success(result);
                }
                log.warn("【取件推荐】LLM 返回为空，降级到规则引擎");
            } catch (Exception e) {
                log.error("【取件推荐】LLM 调用异常，降级到规则引擎：{}", e.getMessage(), e);
            }
        } else {
            log.debug("【取件推荐】LLM 未启用（enabled={}, key={}），直接规则引擎",
                    llmEnabled, StringUtils.hasText(llmApiKey) ? "已配置" : "未配置");
        }

        // 4. 规则引擎降级
        PickupRecommendationVO result = ruleBasedRecommend(ctx);
        log.info("【取件推荐】规则引擎模式，userId={}", userId);
        return R.success(result);
    }

    // ---------------- LLM Function Calling ----------------

    /**
     * 调用 LLM（ChatClient + Function Calling）获取推荐。
     * <p>
     * Spring AI 自动把 {@code @Tool} 标记的方法注册为工具，
     * ChatClient 内部处理「LLM 发起 tool_call → 执行本地方法 → 把结果回传给 LLM → LLM 生成最终回复」的完整循环。
     * </p>
     */
    private PickupRecommendationVO callLlm(Long userId, ContextData ctx) {
        // 关键：Spring AI 不会自动扫描 @Tool 组件，必须在请求上显式注册工具对象
        // （只此一处注册，不使用全局 ToolCallbackProvider / defaultTools，避免重复注册同名工具）
        ChatClient chatClient = chatClientBuilder.build();

        // System prompt：角色、强制工具调用、数据纪律、输出格式
        String systemPrompt = """
                你是快递驿站的智能取件时间推荐助手。请根据用户待取快递、驿站历史取件忙闲、超期阈值、货架使用率，推荐一个最合理的取件时间并解释理由。

                【数据收集 —— 必须执行】
                在给出推荐前，你必须依次调用以下 4 个工具，不得遗漏：
                1. getUserPendingExpress：参数 userId 传当前用户ID，用于了解待取件数量与紧迫度；
                2. getHourlyPickupDistribution：参数 days 传 14，用于判断高峰/低谷时段；
                3. getOverdueThreshold：查询超期阈值；
                4. getStationCapacityStatus：查询当前货架使用率。

                【数据纪律 —— 严格遵守】
                1. 回复中出现的所有数字（件数、小时数、使用率、时段）必须来自工具返回结果，严禁凭空编造、估算或写"系统模拟/假设数据"；
                2. 若工具返回错误或为空，直接回复"暂时无法获取驿站数据，请稍后再试"，不要自行补数据；
                3. 必须遵守驿站营业时间：每日 08:00 ~ 22:00，不得推荐营业时段外或已经过去的时段；
                4. 优先推荐低谷时段；若用户有快递距超期不足 12 小时或已超期，优先建议今天尽快取；货架使用率高于 85% 也建议尽快取；20:00 之后可推荐明日。

                【输出格式 —— 纯文本】
                禁止使用任何 Markdown 语法（不要出现 **、#、反引号）。严格按以下格式输出：
                数据依据：
                - 待取快递：共 X 件（已超期 X 件 / 最近一件距超期约 X 小时）；
                - 驿站忙闲：高峰 HH:00-HH:00，低谷 HH:00-HH:00；
                - 货架使用率：X%；超期阈值：X 小时。
                推荐时段：今日 HH:mm-HH:mm
                推荐理由：
                1. ……
                2. ……
                3. ……
                其中「推荐时段」必须单独占一行，日标签只能写"今日"或"明日"，起止时间用半角连字符连接。若用户无待取快递，推荐时段写明日 09:00-10:00，并说明无需专门跑一趟。
                """;

        String userPrompt = """
                当前用户ID：%d
                现在时间：%s（%s）
                驿站营业时间：每日 08:00 ~ 22:00
                请调用工具收集数据后，为我推荐一个合理的取件时间。
                """.formatted(
                        userId,
                        LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")),
                        DayOfWeek.from(LocalDate.now()).getDisplayName(java.time.format.TextStyle.FULL, Locale.CHINESE)
                );

        // 显式把 4 个 @Tool 方法所在的 bean 注册到本次请求
        String response = chatClient.prompt()
                .system(systemPrompt)
                .user(userPrompt)
                .tools(recommendationTools)
                .call()
                .content();

        log.debug("【取件推荐】LLM 原始回复：{}", response);

        // 把 LLM 自然语言回复包装成 VO（source=LLM）
        return wrapLlmResponse(response, ctx);
    }

    /** 定位「推荐时段：…」整行（避免误匹配数据依据里的高峰/低谷时间范围） */
    private static final java.util.regex.Pattern SLOT_LINE_PATTERN = java.util.regex.Pattern.compile(
            "(?m)^\\s*推荐时段[：:]\\s*(.+?)\\s*$");
    /** 在推荐时段行内解析：日标签(今日/明日) 可省略，HH:mm-HH:mm（兼容 ~、至、全角冒号） */
    private static final java.util.regex.Pattern SLOT_PATTERN = java.util.regex.Pattern.compile(
            "(今日|明日)?\\s*([0-2]?\\d)[:：]([0-5]\\d)\\s*[-~至]\\s*([0-2]?\\d)[:：]([0-5]\\d)");

    private PickupRecommendationVO wrapLlmResponse(String response, ContextData ctx) {
        PickupRecommendationVO vo = new PickupRecommendationVO();
        vo.setSource("LLM");

        // 紧迫度（用真实上下文贴标签，与底部"数据摘要"口径一致）
        if (ctx.hasUrgentExpress) {
            vo.setUrgencyLevel("URGENT");
            vo.setUrgencyHint("有 " + ctx.pendingList.size() + " 件快递临近或已超期，请尽快取件");
        } else if (ctx.capacityLevel == CapacityLevel.TIGHT || ctx.capacityLevel == CapacityLevel.URGENT) {
            vo.setUrgencyLevel("TIGHT");
            vo.setUrgencyHint("驿站货架紧张，建议尽快取件");
        } else if (ctx.pendingList.isEmpty()) {
            vo.setUrgencyLevel("RELAXED");
            vo.setUrgencyHint("当前无待取快递");
        } else {
            vo.setUrgencyLevel("NORMAL");
            vo.setUrgencyHint("取件时间相对自由，可自行安排");
        }

        // 防御性清洗：模型若不听话仍输出 Markdown，去掉 ** 和代码反引号，保证前端纯文本可读
        String cleaned = StringUtils.hasText(response) ? response.trim() : "";
        cleaned = cleaned.replace("**", "").replace("`", "");

        // 从回复中解析 LLM 真实推荐时段；解析失败才用规则引擎兜底（避免顶部时段与正文再次打架）
        RuleSlot effectiveSlot = parseSlotFromLlm(cleaned);
        if (effectiveSlot == null) {
            effectiveSlot = findLowPeakSlot(ctx);
        }
        vo.setRecommendedTimeSlot(effectiveSlot.display);
        vo.setRecommendedStart(effectiveSlot.start.format(TIME_FMT));
        vo.setRecommendedEnd(effectiveSlot.end.format(TIME_FMT));

        // reason 去掉「推荐时段：…」整行（顶部已展示），避免重复；清理多余空行
        String reasonText = cleaned.replaceAll("(?m)^\\s*推荐时段[：:].*$", "")
                .replaceAll("\\n{3,}", "\n\n")
                .trim();
        vo.setReason(StringUtils.hasText(reasonText) ? reasonText : "综合驿站忙闲与你的待取件情况给出的推荐");

        // 上下文
        Map<String, Object> context = new LinkedHashMap<>();
        context.put("pendingCount", ctx.pendingList.size());
        context.put("overdueCount", ctx.overdueCount);
        context.put("hasUrgentExpress", ctx.hasUrgentExpress);
        context.put("capacityLevel", ctx.capacityLevel.name());
        context.put("usageRatePercent", ctx.usageRate);
        vo.setContext(context);

        return vo;
    }

    /**
     * 从 LLM 纯文本回复中解析推荐时段。
     * @return 解析成功的 RuleSlot（含 display / start / end）；失败返回 null
     */
    private RuleSlot parseSlotFromLlm(String text) {
        if (!StringUtils.hasText(text)) {
            return null;
        }
        // 先定位「推荐时段：」所在行，只在该行内容里找时间，避免拿到数据依据中的高峰/低谷范围
        java.util.regex.Matcher lineMatcher = SLOT_LINE_PATTERN.matcher(text);
        if (!lineMatcher.find()) {
            return null;
        }
        java.util.regex.Matcher m = SLOT_PATTERN.matcher(lineMatcher.group(1));
        if (!m.find()) {
            return null;
        }
        String dayLabel = m.group(1);
        LocalTime start = LocalTime.of(Integer.parseInt(m.group(2)), Integer.parseInt(m.group(3)));
        LocalTime end = LocalTime.of(Integer.parseInt(m.group(4)), Integer.parseInt(m.group(5)));
        // 基本合法性：营业时间内、起止合理
        if (start.getHour() < OPEN_HOUR || end.getHour() > CLOSE_HOUR || !end.isAfter(start)) {
            return null;
        }
        String label = (dayLabel != null ? dayLabel : "今日");
        RuleSlot slot = new RuleSlot();
        slot.start = start;
        slot.end = end;
        slot.display = label + " " + start.format(TIME_FMT) + " ~ " + end.format(TIME_FMT);
        slot.belowAvg = true;
        return slot;
    }

    // ---------------- 规则引擎降级 ----------------

    /**
     * 纯规则引擎推荐——不调用任何外部 LLM，完全基于数据库数据 + 固定规则。
     */
    private PickupRecommendationVO ruleBasedRecommend(ContextData ctx) {
        PickupRecommendationVO vo = new PickupRecommendationVO();
        vo.setSource("RULE");

        // 1. 紧迫度
        final String urgencyLevel;
        final String urgencyHint;
        if (ctx.pendingList.isEmpty()) {
            urgencyLevel = "RELAXED";
            urgencyHint = "当前没有待取快递，无需安排取件";
        } else if (ctx.hasUrgentExpress) {
            urgencyLevel = "URGENT";
            urgencyHint = "有快递距超期不足 12 小时或已超期，请尽快取件！";
        } else if (ctx.capacityLevel == CapacityLevel.URGENT || ctx.capacityLevel == CapacityLevel.TIGHT) {
            urgencyLevel = "TIGHT";
            urgencyHint = "驿站货架紧张，建议尽快取件以缓解空间压力";
        } else {
            urgencyLevel = "NORMAL";
            urgencyHint = "取件时间相对自由，可自行安排";
        }
        vo.setUrgencyLevel(urgencyLevel);
        vo.setUrgencyHint(urgencyHint);

        // 2. 主推荐时段
        RuleSlot mainSlot = null;
        if (ctx.pendingList.isEmpty()) {
            // 无待取快递——推荐内容提示即可
            LocalDate tomorrow = LocalDate.now().plusDays(1);
            vo.setRecommendedTimeSlot("暂无需取件的快递");
            vo.setRecommendedStart(tomorrow.atTime(9, 0).format(TIME_FMT));
            vo.setRecommendedEnd(tomorrow.atTime(10, 0).format(TIME_FMT));
            vo.setReason("你当前没有待取快递，无需专门跑一趟驿站。下次有新快递到驿站时再来即可。");
        } else {
            mainSlot = findLowPeakSlot(ctx);
            vo.setRecommendedTimeSlot(mainSlot.display);
            vo.setRecommendedStart(mainSlot.start.format(TIME_FMT));
            vo.setRecommendedEnd(mainSlot.end.format(TIME_FMT));
            vo.setReason(buildRuleReason(ctx, mainSlot));
        }

        // 3. 上下文
        Map<String, Object> context = new LinkedHashMap<>();
        context.put("pendingCount", ctx.pendingList.size());
        context.put("overdueCount", ctx.overdueCount);
        context.put("hasUrgentExpress", ctx.hasUrgentExpress);
        context.put("capacityLevel", ctx.capacityLevel.name());
        context.put("usageRatePercent", ctx.usageRate);
        vo.setContext(context);

        return vo;
    }

    /**
     * 规则理由——一句话解释推荐依据
     */
    private String buildRuleReason(ContextData ctx, RuleSlot slot) {
        StringBuilder sb = new StringBuilder();
        if (ctx.hasUrgentExpress) {
            sb.append("你有 ").append(ctx.pendingList.size()).append(" 件快递待取，其中部分已临近超期，");
        } else {
            sb.append("你有 ").append(ctx.pendingList.size()).append(" 件快递待取，");
        }
        sb.append(slot.display.startsWith("今日") ? "今日" : "明日")
                .append(slot.start.format(TIME_FMT)).append("~").append(slot.end.format(TIME_FMT))
                .append(" 是驿站历史取件低谷时段，")
                .append(slot.belowAvg ? "此时段取件人少、等候时间短" : "时段合理");
        if (ctx.capacityLevel == CapacityLevel.URGENT) {
            sb.append("；驿站货架已满，建议尽快取件以避免新到快递无处存放");
        }
        sb.append("。");
        return sb.toString();
    }

    /**
     * 找一个最佳低谷时段
     */
    private RuleSlot findLowPeakSlot(ContextData ctx) {
        List<Map<Integer, Long>> ranked = rankQuietHours(ctx);
        LocalDate baseDate = LocalDate.now();
        LocalTime now = LocalTime.now();

        // 如果有紧急件或货架紧张——优先推荐今天
        boolean preferToday = ctx.hasUrgentExpress
                || ctx.capacityLevel == CapacityLevel.URGENT
                || ctx.capacityLevel == CapacityLevel.TIGHT;

        LocalDate targetDate = preferToday ? baseDate : baseDate;

        for (Map<Integer, Long> entry : ranked) {
            int hour = entry.keySet().iterator().next();
            long count = entry.values().iterator().next();

            // 只在营业时间内
            if (hour < OPEN_HOUR || hour >= CLOSE_HOUR) {
                continue;
            }

            LocalTime start = LocalTime.of(hour, 0);
            LocalTime end = LocalTime.of(Math.min(hour + 2, CLOSE_HOUR), 0);

            // 如果这个时段在今天已经过了，推到明天
            if (targetDate.atTime(start).isBefore(LocalDateTime.now())) {
                targetDate = baseDate.plusDays(1);
            }

            // 如果 preferToday 但今天的可行时段都过了，只能明天
            String dayLabel = targetDate.equals(baseDate) ? "今日" :
                    targetDate.equals(baseDate.plusDays(1)) ? "明日" : targetDate.format(DATE_FMT);

            RuleSlot slot = new RuleSlot();
            slot.start = start;
            slot.end = end;
            slot.display = dayLabel + " " + start.format(TIME_FMT) + " ~ " + end.format(TIME_FMT);
            slot.belowAvg = count < ctx.avgPickupPerHour;
            return slot;
        }

        // 兜底：推荐明天 9 点
        LocalTime start = LocalTime.of(9, 0);
        RuleSlot slot = new RuleSlot();
        slot.start = start;
        slot.end = LocalTime.of(11, 0);
        slot.display = "明日 " + start.format(TIME_FMT) + " ~ 11:00";
        slot.belowAvg = true;
        return slot;
    }

    /**
     * 按安静程度排序各时段（取件量升序 = 最安静在前）
     */
    private List<Map<Integer, Long>> rankQuietHours(ContextData ctx) {
        // 把 ctx.hourlyDistribution 转为 hour→count 并升序排序
        List<Map.Entry<Integer, Long>> entries = new ArrayList<>(ctx.hourlyDistribution.entrySet());
        entries.sort(Map.Entry.comparingByValue());

        List<Map<Integer, Long>> result = new ArrayList<>();
        for (Map.Entry<Integer, Long> e : entries) {
            Map<Integer, Long> m = new LinkedHashMap<>();
            m.put(e.getKey(), e.getValue());
            result.add(m);
        }
        return result;
    }

    // ---------------- 数据准备 ----------------

    /**
     * 构建推荐上下文数据（无论 LLM 还是规则引擎都要用）
     */
    private ContextData buildContextData(Long userId) {
        ContextData ctx = new ContextData();

        // 1. 用户待取快递
        String phone = getUserPhone(userId);
        ctx.pendingList = expressMapper.selectReceiverList(userId, phone, null, "WAITING_PICKUP");
        ctx.overdueCount = (int) ctx.pendingList.stream()
                .filter(e -> e.getOverdueFlag() != null && e.getOverdueFlag() == 1).count();

        // 2. 按小时的取件分布（补齐 0~23）
        int windowDays = 14;
        List<Map<String, Object>> raw = expressMapper.selectHourlyPickupDistribution(windowDays);
        Map<Integer, Long> hourly = new LinkedHashMap<>();
        for (int h = 0; h < 24; h++) {
            hourly.put(h, 0L);
        }
        for (Map<String, Object> row : raw) {
            hourly.put(((Number) row.get("hour")).intValue(), ((Number) row.get("count")).longValue());
        }
        ctx.hourlyDistribution = hourly;
        ctx.avgPickupPerHour = hourly.values().stream().mapToLong(Long::longValue).sum() / 24.0;

        // 3. 货架使用率
        LocationUsageVO usage = shelfLocationMapper.selectUsage();
        long total = usage != null && usage.getTotal() != null ? usage.getTotal() : 0;
        long used = usage != null && usage.getUsed() != null ? usage.getUsed() : 0;
        ctx.usageRate = total > 0 ? Math.round(used * 10000.0 / total) / 100.0 : 0.0;
        if (ctx.usageRate >= 90) {
            ctx.capacityLevel = CapacityLevel.URGENT;
        } else if (ctx.usageRate >= 75) {
            ctx.capacityLevel = CapacityLevel.TIGHT;
        } else if (ctx.usageRate >= 50) {
            ctx.capacityLevel = CapacityLevel.NORMAL;
        } else {
            ctx.capacityLevel = CapacityLevel.AMPLE;
        }

        // 4. 是否有紧急件（距超期不足 12 小时 或已超期）
        int overdueHours = sysConfigService.getIntValue("station.overdue.hours", DEFAULT_OVERDUE_HOURS);
        LocalDateTime now = LocalDateTime.now();
        ctx.hasUrgentExpress = ctx.pendingList.stream().anyMatch(e -> {
            if (e.getOverdueFlag() != null && e.getOverdueFlag() == 1) {
                return true;
            }
            if (e.getStorageTime() != null) {
                LocalDateTime storage = LocalDateTime.parse(e.getStorageTime().replace(" ", "T"));
                LocalDateTime deadline = storage.plusHours(overdueHours);
                long hoursLeft = ChronoUnit.HOURS.between(now, deadline);
                return hoursLeft > 0 && hoursLeft <= 12;
            }
            return false;
        });

        return ctx;
    }

    /**
     * 获取用户手机号（用于 selectReceiverList 的 phone 联合匹配）。
     * 实际上 selectReceiverList 已用 receiver_user_id 匹配，phone 是兜底的 OR 条件，传 null 即可。
     */
    private String getUserPhone(Long userId) {
        return null;
    }

    // ---------------- 内部类 ----------------

    /** 规则引擎推荐出的时段 */
    private static class RuleSlot {
        LocalTime start;
        LocalTime end;
        String display;
        boolean belowAvg;   // 该时段取件量低于历史均值
    }

    /** 推荐上下文数据 */
    private static class ContextData {
        List<ExpressVO> pendingList = new ArrayList<>();
        int overdueCount;
        boolean hasUrgentExpress;
        Map<Integer, Long> hourlyDistribution = new LinkedHashMap<>();
        double avgPickupPerHour;
        double usageRate;
        CapacityLevel capacityLevel = CapacityLevel.NORMAL;
    }

    private enum CapacityLevel { AMPLE, NORMAL, TIGHT, URGENT }
}
