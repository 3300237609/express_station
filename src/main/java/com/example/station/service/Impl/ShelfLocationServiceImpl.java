package com.example.station.service.Impl;

import com.example.station.dto.ShelfLocationDTO;
import com.example.station.entity.ShelfLocation;
import com.example.station.mapper.ShelfLocationMapper;
import com.example.station.mapper.ShelfMapper;
import com.example.station.service.ShelfLocationService;
import com.example.station.common.R;
import com.example.station.vo.ShelfLocationVO;
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
 * 货位管理业务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ShelfLocationServiceImpl implements ShelfLocationService {

    private final ShelfLocationMapper shelfLocationMapper;
    private final ShelfMapper shelfMapper;

    @Override
    public R list(Long shelfId, Integer status, Integer pageNum, Integer pageSize) {
        // 1. 分页参数兜底（PageHelper 默认第 1 页、每页 10 条）
        pageNum = pageNum == null ? 1 : pageNum;
        pageSize = pageSize == null ? 10 : pageSize;
        // 2. 分页查询
        PageHelper.startPage(pageNum, pageSize);
        List<ShelfLocationVO> list = shelfLocationMapper.selectList(shelfId, status);
        PageInfo<ShelfLocationVO> pageInfo = new PageInfo<>(list);
        return R.success(list).add("total", pageInfo.getTotal());
    }

    @Override
    public R detail(Long id) {
        // 1. 主键必填
        if (id == null) {
            return R.error("货位ID不能为空！");
        }
        // 2. 存在性校验
        ShelfLocationVO vo = shelfLocationMapper.selectById(id);
        if (vo == null) {
            return R.error("货位不存在！");
        }
        return R.success(vo);
    }

    @Override
    public R freeList(Long shelfId, Integer pageNum, Integer pageSize) {
        // 1. 分页参数兜底
        pageNum = pageNum == null ? 1 : pageNum;
        pageSize = pageSize == null ? 10 : pageSize;
        // 2. 分页查询空闲货位
        PageHelper.startPage(pageNum, pageSize);
        List<ShelfLocationVO> list = shelfLocationMapper.selectFreeList(shelfId);
        PageInfo<ShelfLocationVO> pageInfo = new PageInfo<>(list);
        return R.success(list).add("total", pageInfo.getTotal());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public R add(ShelfLocationDTO dto) {
        // 1. 兜底空值校验
        if (dto.getShelfId() == null) {
            return R.error("所属货架不能为空！");
        }
        if (!StringUtils.hasText(dto.getLocationNo())) {
            return R.error("货位编号不能为空！");
        }
        // 2. 所属货架存在性校验
        if (shelfMapper.selectById(dto.getShelfId()) == null) {
            return R.error("所属货架不存在！");
        }
        // 3. 货位编号查重
        String locationNo = dto.getLocationNo().trim();
        if (shelfLocationMapper.countByLocationNo(locationNo, null) > 0) {
            return R.error("货位编号已存在！");
        }
        // 4. 同一货架内行列号查重
        if (shelfLocationMapper.countByShelfIdRowCol(dto.getShelfId(), dto.getRowNo(), dto.getColumnNo(), null) > 0) {
            return R.error("该货架已存在相同行列的货位！");
        }
        // 5. 落库并同步货架总货位数 +1
        LocalDateTime now = LocalDateTime.now();
        ShelfLocation location = new ShelfLocation();
        location.setShelfId(dto.getShelfId());
        location.setLocationNo(locationNo);
        location.setRowNo(dto.getRowNo());
        location.setColumnNo(dto.getColumnNo());
        location.setCapacity(dto.getCapacity());
        location.setUsedCount(0);
        location.setStatus(1);
        location.setCreateTime(now);
        location.setUpdateTime(now);
        shelfLocationMapper.insert(location);
        shelfMapper.increaseCapacity(dto.getShelfId(), now);
        log.info("【货位】新增成功：id={}，locationNo={}", location.getId(), locationNo);
        return R.success("新增成功！");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public R update(ShelfLocationDTO dto) {
        // 1. 主键必填与兜底空值校验
        if (dto.getId() == null) {
            return R.error("货位ID不能为空！");
        }
        if (dto.getShelfId() == null) {
            return R.error("所属货架不能为空！");
        }
        if (!StringUtils.hasText(dto.getLocationNo())) {
            return R.error("货位编号不能为空！");
        }
        // 2. 存在性校验
        ShelfLocationVO current = shelfLocationMapper.selectById(dto.getId());
        if (current == null) {
            return R.error("货位不存在！");
        }
        // 3. 所属货架不可迁移（保证货架容量计数一致，如需迁移请删除重建）
        if (!dto.getShelfId().equals(current.getShelfId())) {
            return R.error("所属货架不可修改，如需迁移请删除后重建！");
        }
        // 4. 容量不得小于已使用数量
        if (dto.getCapacity() < current.getUsedCount()) {
            return R.error("货位容量不能小于已使用数量！");
        }
        // 5. 货位编号查重（排除自身）
        String locationNo = dto.getLocationNo().trim();
        if (shelfLocationMapper.countByLocationNo(locationNo, dto.getId()) > 0) {
            return R.error("货位编号已存在！");
        }
        // 6. 同一货架内行列号查重（排除自身）
        if (shelfLocationMapper.countByShelfIdRowCol(dto.getShelfId(), dto.getRowNo(), dto.getColumnNo(), dto.getId()) > 0) {
            return R.error("该货架已存在相同行列的货位！");
        }
        // 7. 更新基础信息 + 自动重算动态状态（停用状态由管理员手动控制，不在此覆盖）
        LocalDateTime now = LocalDateTime.now();
        ShelfLocation location = new ShelfLocation();
        location.setId(dto.getId());
        location.setLocationNo(locationNo);
        location.setRowNo(dto.getRowNo());
        location.setColumnNo(dto.getColumnNo());
        location.setCapacity(dto.getCapacity());
        location.setUpdateTime(now);
        // 扩容/缩容后，根据 used_count 与新 capacity 自动推算 status
        // status=4(停用) 为人工设置，跳过重算
        Integer newStatus = current.getStatus() != null && current.getStatus() == 4
                ? 4
                : computeStatus(current.getUsedCount(), dto.getCapacity());
        location.setStatus(newStatus);
        shelfLocationMapper.update(location);
        return R.success("修改成功！");
    }

    /**
     * 根据已用数量与容量推算货位动态状态
     *
     * @param used    已使用数量
     * @param capacity 总容量
     * @return 1=空闲 / 2=部分占用 / 3=已满
     */
    private int computeStatus(Integer used, Integer capacity) {
        int u = used == null ? 0 : used;
        if (u == 0) return 1;           // 空闲
        if (u >= capacity) return 3;    // 已满
        return 2;                        // 部分占用
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public R delete(Long id) {
        // 1. 主键必填
        if (id == null) {
            return R.error("货位ID不能为空！");
        }
        // 2. 存在性校验
        ShelfLocationVO current = shelfLocationMapper.selectById(id);
        if (current == null) {
            return R.error("货位不存在！");
        }
        // 3. 引用守卫：被占用的货位禁止删除
        if (current.getUsedCount() != null && current.getUsedCount() > 0) {
            return R.error("该货位已被占用，不允许删除！");
        }
        // 4. 物理删除（表无逻辑删除列）并同步货架总货位数 -1
        shelfLocationMapper.delete(id);
        shelfMapper.decreaseCapacity(current.getShelfId(), LocalDateTime.now());
        log.info("【货位】删除成功：id={}，locationNo={}", id, current.getLocationNo());
        return R.success("删除成功！");
    }

    @Override
    public R updateStatus(Long id, Integer status) {
        // 1. 参数校验（1 空闲，2 部分占用，3 已满，4 停用）
        if (id == null) {
            return R.error("货位ID不能为空！");
        }
        if (status == null || status < 1 || status > 4) {
            return R.error("状态值不正确，只能为1空闲、2部分占用、3已满或4停用！");
        }
        // 2. 存在性校验
        if (shelfLocationMapper.selectById(id) == null) {
            return R.error("货位不存在！");
        }
        // 3. 更新状态（管理员手动修正）
        shelfLocationMapper.updateStatus(id, status, LocalDateTime.now());
        return R.success("状态修改成功！");
    }

    @Override
    public R gridFull(Long shelfId) {
        // shelfId 可空：空则查全部货架，非空则校验货架存在
        if (shelfId != null && shelfMapper.selectById(shelfId) == null) {
            return R.error("货架不存在！");
        }
        // 直接查全量（不调 PageHelper），用于 SVG 网格渲染
        List<ShelfLocationVO> list = shelfLocationMapper.selectList(shelfId, null);
        return R.success(list);
    }
}
