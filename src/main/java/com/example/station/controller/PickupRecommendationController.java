package com.example.station.controller;

import com.example.station.common.R;
import com.example.station.service.PickupRecommendationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 取件时间智能推荐接口
 * <p>
 * 通过 Function Calling（LLM + 工具函数）分析驿站忙闲、用户待取快递、超期阈值、货架紧张度，
 * 推荐最合理的取件时段；LLM 不可用时自动降级为规则引擎。
 * </p>
 * <p>
 * 权限：登录即可（authenticated），任何已登录用户都可以为自己查询推荐。
 * </p>
 */
@RestController
@RequestMapping("/pickup/recommendation")
@RequiredArgsConstructor
public class PickupRecommendationController {

    private final PickupRecommendationService pickupRecommendationService;

    /**
     * 获取当前用户的取件时间推荐
     *
     * @return 推荐结果：包含主推荐时段、理由、紧迫度标签、备选方案、数据来源标识（LLM 或 RULE 降级）
     */
    @GetMapping
    public R recommend() {
        return pickupRecommendationService.recommend();
    }
}
