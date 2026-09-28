package com.example.station.service.Impl;

import com.example.station.common.R;
import com.example.station.annotation.constant.ExpressConstants;
import com.example.station.annotation.constant.NotificationConstants;
import com.example.station.dto.InboundDTO;
import com.example.station.entity.Express;
import com.example.station.mapper.ExpressMapper;
import com.example.station.mapper.ShelfLocationMapper;
import com.example.station.mapper.ShelfMapper;
import com.example.station.service.ExpressService;
import com.example.station.service.InboundService;
import com.example.station.service.NotificationService;
import com.example.station.vo.ExpressVO;
import com.example.station.vo.ShelfLocationVO;
import com.example.station.vo.ShelfVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

/**
 * 快递入库业务实现
 * 写序规则：全部业务校验在前，首个写操作失败时无任何变更可直接返回业务错误；
 * 首个写操作之后的失败一律抛异常触发事务回滚（不使用 return R.error）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InboundServiceImpl implements InboundService {

    private final ExpressMapper expressMapper;
    private final ShelfLocationMapper shelfLocationMapper;
    private final ShelfMapper shelfMapper;
    private final ExpressService expressService;
    private final NotificationService notificationService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public R add(InboundDTO dto) {
        // 1. 入参必填校验
        if (dto == null || dto.getLocationId() == null) {
            return R.error("货位不能为空！");
        }
        // 2. 创建分支的快递报文校验（字段必填、关联校验、单号查重，纯读前置：先报数据错误再报货位资源错误）
        if (dto.getExpressId() == null) {
            String baseError = validateBase(dto);
            if (baseError != null) {
                return R.error(baseError);
            }
            if (expressMapper.countByExpressNo(dto.getExpressNo().trim(), null) > 0) {
                return R.error("快递单号已存在！");
            }
        }
        // 3. 货位存在性与状态校验
        ShelfLocationVO location = shelfLocationMapper.selectById(dto.getLocationId());
        if (location == null) {
            return R.error("货位不存在！");
        }
        if (location.getStatus() != null && location.getStatus() == 4) {
            return R.error("该货位已停用！");
        }
        // 4. 所属货架可用性校验
        ShelfVO shelf = shelfMapper.selectById(location.getShelfId());
        if (shelf == null) {
            return R.error("所属货架不存在！");
        }
        if (shelf.getStatus() == null || shelf.getStatus() != 1) {
            return R.error("所属货架已停用，不能入库！");
        }
        // 5. 容量预检（并发抢占由 increaseUsed 行级守卫兜底）
        if (location.getUsedCount() != null && location.getCapacity() != null
                && location.getUsedCount() >= location.getCapacity()) {
            return R.error("货位已满，请重新选择货位！");
        }
        LocalDateTime now = LocalDateTime.now();
        Long expressId;
        if (dto.getExpressId() != null) {
            // 4a. 绑定已录入快递：存在性与状态校验
            ExpressVO current = expressMapper.selectById(dto.getExpressId());
            if (current == null) {
                return R.error("快递不存在！");
            }
            if (!ExpressConstants.STATUS_WAITING.equals(current.getStatus())) {
                return R.error("该快递当前状态不可入库！");
            }
            if (current.getLocationId() != null) {
                return R.error("该快递已关联货位，不能重复入库！");
            }
            expressId = current.getId();
            // 5a. 占用货位（首个写操作：失败时无任何变更）
            if (shelfLocationMapper.increaseUsed(location.getId(), now) == 0) {
                return R.error("货位已满，请重新选择货位！");
            }
            // 6a. 绑定快递（状态守卫；affected=0 属并发变更，抛异常回滚货位占用）
            int rows = expressMapper.updateInbound(expressId, location.getId(),
                    ExpressConstants.STATUS_WAITING_PICKUP, now, ExpressConstants.STATUS_WAITING, now);
            if (rows == 0) {
                throw new IllegalStateException("快递入库绑定失败：并发状态变更，请重试");
            }
        } else {
            // 4b. 直接入库创建（报文与单号查重已在步骤 2 前置校验）
            String expressNo = dto.getExpressNo().trim();
            // 5b. 生成唯一取件码（复用快递模块，占号口径一致）
            String pickupCode = expressService.generatePickupCode();
            if (pickupCode == null) {
                return R.error("取件码生成失败，请稍后重试！");
            }
            // 6b. 占用货位（首个写操作：失败时无任何变更）
            if (shelfLocationMapper.increaseUsed(location.getId(), now) == 0) {
                return R.error("货位已满，请重新选择货位！");
            }
            // 7b. 创建快递（直接进入待取件并记录入库时间；唯一键冲突抛异常回滚货位占用）
            Express express = new Express();
            express.setExpressNo(expressNo);
            express.setCompanyId(dto.getCompanyId());
            express.setSenderName(dto.getSenderName());
            express.setSenderPhone(dto.getSenderPhone());
            express.setReceiverName(dto.getReceiverName().trim());
            express.setReceiverPhone(dto.getReceiverPhone().trim());
            express.setReceiverUserId(dto.getReceiverUserId());
            express.setPickupCode(pickupCode);
            express.setLocationId(location.getId());
            express.setStatus(ExpressConstants.STATUS_WAITING_PICKUP);
            express.setSizeType(dto.getSizeType());
            express.setWeight(dto.getWeight());
            express.setArrivalTime(dto.getArrivalTime() != null ? dto.getArrivalTime() : now);
            express.setStorageTime(now);
            express.setOverdueFlag(0);
            express.setRemark(dto.getRemark());
            express.setDeleted(0);
            express.setCreateTime(now);
            express.setUpdateTime(now);
            expressMapper.insert(express);
            expressId = express.getId();
        }
        // 8. 自愈式刷新货架已使用货位数
        shelfMapper.refreshUsed(location.getShelfId(), now);
        // 9. 查询入库结果并通知收件用户（通知开关由 NotificationService 把控，随事务提交）
        ExpressVO vo = expressMapper.selectById(expressId);
        if (vo != null) {
            String content = "您的快递（单号 " + vo.getExpressNo() + "）已入库，取件码：" + vo.getPickupCode()
                    + "，货位：" + vo.getLocationNo() + "，请及时取件。";
            notificationService.send(vo.getReceiverUserId(), expressId,
                    "快递入库通知", content, NotificationConstants.TYPE_PICKUP_NOTICE);
        }
        log.info("【入库】完成入库：expressId={}，locationId={}，shelfId={}",
                expressId, location.getId(), location.getShelfId());
        return R.success(vo);
    }

    /**
     * 入库创建路径的快递基础字段校验（与快递新增同口径）
     * @param dto 入库入参
     * @return 错误文案，全部通过返回 null
     */
    private String validateBase(InboundDTO dto) {
        if (!StringUtils.hasText(dto.getExpressNo())) {
            return "快递单号不能为空！";
        }
        if (!StringUtils.hasText(dto.getReceiverName())) {
            return "收件人不能为空！";
        }
        if (!StringUtils.hasText(dto.getReceiverPhone())) {
            return "收件人电话不能为空！";
        }
        if (dto.getCompanyId() == null) {
            return "快递公司不能为空！";
        }
        // 复用快递模块关联校验（公司存在、收件用户存在、尺寸枚举）
        return expressService.validateExpressInfo(dto);
    }
}
