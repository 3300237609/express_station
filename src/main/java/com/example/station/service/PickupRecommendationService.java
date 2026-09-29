package com.example.station.service;

import com.example.station.common.R;

/**
 * 取件时间推荐业务接口
 * <p>
 * 通过 Function Calling（LLM + 工具函数）分析驿站忙闲、用户待取快递、超期阈值、货架紧张度，
 * 给出「什么时间去取件最合理」的推荐；LLM 不可用时自动降级为规则引擎。
 * </p>
 */
public interface PickupRecommendationService {

    /**
     * 获取当前用户的取件时间推荐
     *
     * @return 推荐结果，包含推荐时段、理由、紧迫度、备选方案
     */
    R recommend();
}
