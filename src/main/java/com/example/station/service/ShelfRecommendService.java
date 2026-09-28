package com.example.station.service;

import com.example.station.common.R;

import java.math.BigDecimal;

/**
 * 智能货位推荐业务接口（与快递模块解耦，未来可替换推荐算法）
 */
public interface ShelfRecommendService {

    /**
     * 按快递属性推荐货位（过滤有剩余容量的正常货位，按评分降序返回全部候选）
     * 评分 = 容量匹配(40) + 空闲程度(40) + 区域匹配(20)，满分 100
     * @param sizeType 尺寸类型：SMALL/MEDIUM/LARGE，可空（缺省按 SMALL 需求计算）
     * @param weight 重量 kg，可空（超过 10kg 时需求位加 1）
     * @param area 期望区域（如 A 区），可空（不传区域项折半计 10 分）
     * @return 推荐货位列表（附评分与推荐理由），无可用货位返回空列表
     */
    R recommend(String sizeType, BigDecimal weight, String area);
}
