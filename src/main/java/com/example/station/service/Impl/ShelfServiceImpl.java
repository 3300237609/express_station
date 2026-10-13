package com.example.station.service.Impl;

import com.example.station.dto.ShelfDTO;
import com.example.station.entity.Shelf;
import com.example.station.mapper.ShelfLocationMapper;
import com.example.station.mapper.ShelfMapper;
import com.example.station.service.CommonCacheService;
import com.example.station.service.ShelfService;
import com.example.station.common.R;
import com.example.station.vo.ShelfVO;
import com.fasterxml.jackson.core.type.TypeReference;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 货架管理业务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ShelfServiceImpl implements ShelfService {

    private final ShelfMapper shelfMapper;
    private final ShelfLocationMapper shelfLocationMapper;
    private final CommonCacheService cache;

    @Override
    public R list(String keyword, Integer status, Integer pageNum, Integer pageSize) {
        // 1. 分页参数兜底（PageHelper 默认第 1 页、每页 10 条）
        pageNum = pageNum == null ? 1 : pageNum;
        pageSize = pageSize == null ? 10 : pageSize;
        String keywordTrim = StringUtils.hasText(keyword) ? keyword.trim() : null;
        // 1.5 先查缓存
        String cacheKey = cache.shelfListKey(keywordTrim, status, pageNum, pageSize);
        CommonCacheService.CachedPage<ShelfVO> cached = cache.get(cacheKey,
                new TypeReference<CommonCacheService.CachedPage<ShelfVO>>() {
                });
        if (cached != null) {
            return R.success(cached.list()).add("total", cached.total());
        }
        // 2. 分页查询
        PageHelper.startPage(pageNum, pageSize);
        List<ShelfVO> list = shelfMapper.selectList(keywordTrim, status);
        PageInfo<ShelfVO> pageInfo = new PageInfo<>(list);
        // 3. 计算每个货架的货位使用率
        list.forEach(this::fillUsageRate);
        // 3.5 写入缓存（入库/取件后会被清，命中率靠读密集期）
        cache.put(cacheKey, new CommonCacheService.CachedPage<>(list, pageInfo.getTotal(),
                pageInfo.getPageNum(), pageInfo.getPageSize(), pageInfo.getPages()),
                CommonCacheService.TTL_LIST);
        return R.success(list).add("total", pageInfo.getTotal());
    }

    @Override
    public R detail(Long id) {
        // 1. 主键必填
        if (id == null) {
            return R.error("货架ID不能为空！");
        }
        // 2. 先查缓存，未命中查库并回填
        ShelfVO vo = cache.get(cache.shelfDetailKey(id), ShelfVO.class);
        boolean fromDb = false;
        if (vo == null) {
            vo = shelfMapper.selectById(id);
            fromDb = true;
        }
        if (vo == null) {
            return R.error("货架不存在！");
        }
        // 3. 计算货位使用率（缓存命中时已是回填后的结果，仅查库时补算）
        if (fromDb) {
            fillUsageRate(vo);
            cache.put(cache.shelfDetailKey(id), vo, CommonCacheService.TTL_DETAIL);
        }
        return R.success(vo);
    }

    @Override
    public R add(ShelfDTO dto) {
        // 1. 兜底空值校验
        if (!StringUtils.hasText(dto.getShelfNo())) {
            return R.error("货架编号不能为空！");
        }
        if (!StringUtils.hasText(dto.getArea())) {
            return R.error("所属区域不能为空！");
        }
        // 2. 货架编号查重
        String shelfNo = dto.getShelfNo().trim();
        if (shelfMapper.countByShelfNo(shelfNo, null) > 0) {
            return R.error("货架编号已存在！");
        }
        // 3. 落库（总货位数与已用数由货位增删维护，初始为 0）
        Shelf shelf = new Shelf();
        shelf.setShelfNo(shelfNo);
        shelf.setArea(dto.getArea().trim());
        shelf.setCapacity(0);
        shelf.setUsedCount(0);
        shelf.setStatus(1);
        shelf.setRemark(dto.getRemark());
        shelf.setDeleted(0);
        shelf.setCreateTime(LocalDateTime.now());
        shelf.setUpdateTime(LocalDateTime.now());
        shelfMapper.insert(shelf);
        log.info("【货架】新增成功：id={}，shelfNo={}", shelf.getId(), shelfNo);
        cache.evictPrefixAfterCommit(CommonCacheService.SHELF_PREFIX);
        return R.success("新增成功！");
    }

    @Override
    public R update(ShelfDTO dto) {
        // 1. 主键必填
        if (dto.getId() == null) {
            return R.error("货架ID不能为空！");
        }
        if (!StringUtils.hasText(dto.getShelfNo())) {
            return R.error("货架编号不能为空！");
        }
        if (!StringUtils.hasText(dto.getArea())) {
            return R.error("所属区域不能为空！");
        }
        // 2. 存在性校验
        if (shelfMapper.selectById(dto.getId()) == null) {
            return R.error("货架不存在！");
        }
        // 3. 货架编号查重（排除自身）
        String shelfNo = dto.getShelfNo().trim();
        if (shelfMapper.countByShelfNo(shelfNo, dto.getId()) > 0) {
            return R.error("货架编号已存在！");
        }
        // 4. 仅更新基础信息（不触碰 status 与系统维护的容量计数）
        Shelf shelf = new Shelf();
        shelf.setId(dto.getId());
        shelf.setShelfNo(shelfNo);
        shelf.setArea(dto.getArea().trim());
        shelf.setRemark(dto.getRemark());
        shelf.setUpdateTime(LocalDateTime.now());
        shelfMapper.update(shelf);
        cache.evictPrefixAfterCommit(CommonCacheService.SHELF_PREFIX);
        return R.success("修改成功！");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public R delete(Long id) {
        // 1. 主键必填
        if (id == null) {
            return R.error("货架ID不能为空！");
        }
        // 2. 存在性校验
        if (shelfMapper.selectById(id) == null) {
            return R.error("货架不存在！");
        }
        // 3. 引用守卫：名下存在被占用的货位禁止删除
        if (shelfLocationMapper.countOccupiedByShelfId(id) > 0) {
            return R.error("该货架存在占用货位，不允许删除，请先清空货位！");
        }
        // 4. 级联删除空货位，再逻辑删除货架
        shelfLocationMapper.deleteByShelfId(id);
        shelfMapper.delete(id, LocalDateTime.now());
        log.info("【货架】删除成功：id={}", id);
        cache.evictPrefixAfterCommit(CommonCacheService.SHELF_PREFIX, CommonCacheService.LOCATION_PREFIX);
        return R.success("删除成功！");
    }

    @Override
    public R updateStatus(Long id, Integer status) {
        // 1. 参数校验
        if (id == null) {
            return R.error("货架ID不能为空！");
        }
        if (status == null || (status != 0 && status != 1)) {
            return R.error("状态值不正确，只能为1正常或0停用！");
        }
        // 2. 存在性校验
        if (shelfMapper.selectById(id) == null) {
            return R.error("货架不存在！");
        }
        // 3. 更新状态
        shelfMapper.updateStatus(id, status, LocalDateTime.now());
        cache.evictPrefixAfterCommit(CommonCacheService.SHELF_PREFIX);
        return R.success(status == 1 ? "启用成功！" : "停用成功！");
    }

    /**
     * 计算并填充货位使用率（已用货位 / 总货位，保留两位小数）
     */
    private void fillUsageRate(ShelfVO vo) {
        if (vo.getCapacity() == null || vo.getCapacity() <= 0) {
            vo.setUsageRate("0.00%");
            return;
        }
        double rate = vo.getUsedCount() * 100.0 / vo.getCapacity();
        vo.setUsageRate(String.format("%.2f%%", rate));
    }
}
