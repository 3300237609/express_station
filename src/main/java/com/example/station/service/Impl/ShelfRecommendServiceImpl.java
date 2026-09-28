package com.example.station.service.Impl;

import com.example.station.common.R;
import com.example.station.annotation.constant.ExpressConstants;
import com.example.station.mapper.ShelfLocationMapper;
import com.example.station.service.ShelfRecommendService;
import com.example.station.vo.LocationRecommendVO;
import com.example.station.vo.ShelfLocationVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * 智能货位推荐业务实现
 * 过滤有剩余容量的正常货位后按「容量匹配 + 空闲程度 + 区域匹配」打分排序
 */
@Service
@RequiredArgsConstructor
public class ShelfRecommendServiceImpl implements ShelfRecommendService {

    /** 容量匹配满分 */
    private static final int SCORE_FIT = 40;

    /** 空闲程度满分 */
    private static final int SCORE_EMPTY = 40;

    /** 区域匹配满分（不指定区域时折半） */
    private static final int SCORE_AREA = 20;

    /** 超过该重量视为大件，需求位加 1 */
    private static final BigDecimal HEAVY_WEIGHT = new BigDecimal("10");

    private final ShelfLocationMapper shelfLocationMapper;

    @Override
    public R recommend(String sizeType, BigDecimal weight, String area) {
        // 1. 尺寸枚举校验与条件归一
        String sizeTrim = StringUtils.hasText(sizeType) ? sizeType.trim() : null;
        if (sizeTrim != null && !ExpressConstants.SIZE_SET.contains(sizeTrim)) {
            return R.error("尺寸类型不正确，只能为 SMALL、MEDIUM 或 LARGE！");
        }
        String areaTrim = StringUtils.hasText(area) ? area.trim() : null;
        // 2. 候选集：SQL 已过滤停用/已满/货架停用的货位
        List<ShelfLocationVO> candidates = shelfLocationMapper.selectRecommendList();
        List<LocationRecommendVO> result = new ArrayList<>(candidates.size());
        for (ShelfLocationVO location : candidates) {
            result.add(score(location, sizeTrim, weight, areaTrim));
        }
        // 3. 排序：评分降序，同分按货架、行、列稳定排序
        result.sort(Comparator
                .comparingInt(LocationRecommendVO::getScore).reversed()
                .thenComparing(LocationRecommendVO::getShelfId)
                .thenComparing(LocationRecommendVO::getRowNo)
                .thenComparing(LocationRecommendVO::getColumnNo));
        return R.success(result);
    }

    /**
     * 计算单个货位的推荐评分与理由
     * @param location 货位信息
     * @param sizeType 尺寸类型，可空
     * @param weight 重量，可空
     * @param area 期望区域，可空
     * @return 推荐出参（评分 + 理由）
     */
    private LocationRecommendVO score(ShelfLocationVO location, String sizeType, BigDecimal weight, String area) {
        int capacity = location.getCapacity() == null ? 1 : location.getCapacity();
        int used = location.getUsedCount() == null ? 0 : location.getUsedCount();
        int remaining = Math.max(capacity - used, 0);
        int required = requiredSlots(sizeType, weight);

        // 1. 容量匹配：容量足以容纳该尺寸（含超重加位）
        boolean fit = capacity >= required;
        List<String> reasons = new ArrayList<>(3);
        if (fit) {
            reasons.add("容量匹配");
        }

        // 2. 空闲程度：越空闲分越高
        int emptyScore = (int) Math.round((double) SCORE_EMPTY * remaining / capacity);
        if (used == 0) {
            reasons.add("货位空闲");
        }

        // 3. 区域匹配：指定区域命中得满分，不指定折半
        boolean areaMatch = area != null && area.equals(location.getArea());
        int areaScore;
        if (area == null) {
            areaScore = SCORE_AREA / 2;
        } else {
            areaScore = areaMatch ? SCORE_AREA : 0;
            if (areaMatch) {
                reasons.add("区域匹配");
            }
        }

        LocationRecommendVO vo = new LocationRecommendVO();
        vo.setShelfId(location.getShelfId());
        vo.setShelfNo(location.getShelfNo());
        vo.setArea(location.getArea());
        vo.setLocationId(location.getId());
        vo.setLocationNo(location.getLocationNo());
        vo.setRowNo(location.getRowNo());
        vo.setColumnNo(location.getColumnNo());
        vo.setCapacity(capacity);
        vo.setUsedCount(used);
        vo.setRemaining(remaining);
        vo.setStatus(location.getStatus());
        vo.setScore((fit ? SCORE_FIT : 0) + emptyScore + areaScore);
        vo.setReason(reasons.isEmpty() ? "可存放" : String.join("、", reasons));
        return vo;
    }

    /**
     * 计算快递所需货位数（尺寸基数：SMALL/MEDIUM 缺省 1、MEDIUM 2、LARGE 3；超重加 1）
     * @param sizeType 尺寸类型，可空
     * @param weight 重量，可空
     * @return 所需货位数
     */
    private int requiredSlots(String sizeType, BigDecimal weight) {
        int base = 1;
        if ("MEDIUM".equals(sizeType)) {
            base = 2;
        } else if ("LARGE".equals(sizeType)) {
            base = 3;
        }
        boolean heavy = weight != null && weight.compareTo(HEAVY_WEIGHT) > 0;
        return base + (heavy ? 1 : 0);
    }
}
