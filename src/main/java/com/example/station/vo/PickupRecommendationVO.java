package com.example.station.vo;

import lombok.Data;

import java.util.Map;

/**
 * 取件时间推荐结果 VO
 * <p>
 * 同时兼容 LLM Function Calling 结果和规则引擎降级结果——两者共用此结构，
 * 前端无需关心后端是 LLM 还是规则引擎，只看 data 即可。
 * </p>
 */
@Data
public class PickupRecommendationVO {

    /**
     * 本次推荐的数据来源标识，便于前端/运维区分：
     * <ul>
     *   <li>LLM —— LLM Function Calling 结果</li>
     *   <li>RULE —— 规则引擎降级结果（LLM 不可用时）</li>
     * </ul>
     */
    private String source;

    /**
     * 紧迫度标签：URGENT 紧急（超期临近）/ TIGHT 较紧 / NORMAL 正常 / RELAXED 宽松
     */
    private String urgencyLevel;

    /**
     * 紧迫度的中文描述（一句人话），例如「有 2 件快递距超期不足 6 小时，请尽快取件」
     */
    private String urgencyHint;

    /**
     * 主推荐时段描述，例如「今日 14:00 ~ 16:00」或「明日上午 09:30 ~ 11:00」
     */
    private String recommendedTimeSlot;

    /**
     * 主推荐时段的具体起止时间（ISO 格式），用于前端显示倒计时或预约
     */
    private String recommendedStart;
    private String recommendedEnd;

    /**
     * 主推荐理由（自然语言），例如「此时段取件人少，等候时间短，且你的快递还剩 18 小时才超期」
     */
    private String reason;

    /**
     * 本次推荐考虑到的数据摘要，用于前端向用户解释「为什么推荐这个时间」
     */
    private Map<String, Object> context;
}
