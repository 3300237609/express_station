package com.example.station.service.Impl;

import com.example.station.common.R;
import com.example.station.annotation.constant.ExpressConstants;
import com.example.station.annotation.constant.NotificationConstants;
import com.example.station.annotation.constant.RoleConstants;
import com.example.station.dto.PickupDTO;
import com.example.station.entity.PickupRecord;
import com.example.station.mapper.ExpressMapper;
import com.example.station.mapper.PickupRecordMapper;
import com.example.station.mapper.ShelfLocationMapper;
import com.example.station.mapper.ShelfMapper;
import com.example.station.service.ExpressCacheService;
import com.example.station.service.NotificationService;
import com.example.station.service.PickupService;
import com.example.station.utils.SecurityUtils;
import com.example.station.vo.ExpressVO;
import com.example.station.vo.PickupRecordVO;
import com.example.station.vo.ShelfLocationVO;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 取件（出库）业务实现
 * 写序规则：全部业务校验与写前预读在前；首个写操作（updatePickup 带状态守卫）失败时无任何变更；
 * 其后的失败（释放货位数据不一致、取件记录唯一键冲突）一律抛异常触发事务回滚
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PickupServiceImpl implements PickupService {

    /** 通知内容时间格式 */
    private static final DateTimeFormatter CONTENT_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final ExpressMapper expressMapper;
    private final ShelfLocationMapper shelfLocationMapper;
    private final ShelfMapper shelfMapper;
    private final PickupRecordMapper pickupRecordMapper;
    private final NotificationService notificationService;
    private final ExpressCacheService expressCache;

    /**
     * 取件校验结果（快递信息与错误文案二选一）
     * @param express 校验通过的快递信息，失败为 null
     * @param error 错误文案，通过为 null
     */
    private record PickupCheck(ExpressVO express, String error) {
    }

    @Override
    public R verify(String code) {
        PickupCheck check = loadForPickup(code);
        if (check.error() != null) {
            return R.error(check.error());
        }
        return R.success(check.express());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public R confirm(PickupDTO dto) {
        // 1. 取件码验证与状态/归属守卫（事务内复查，防绕过 verify 直接提交）
        PickupCheck check = loadForPickup(dto.getCode());
        if (check.error() != null) {
            return R.error(check.error());
        }
        ExpressVO vo = check.express();
        // 2. 取件方式：普通用户固定 CODE（防伪造工作人员方式），工作人员可选合法枚举
        boolean staff = !RoleConstants.USER.equals(SecurityUtils.getCurrentRole());
        String pickupMethod = StringUtils.hasText(dto.getPickupMethod()) ? dto.getPickupMethod().trim() : "CODE";
        if (!staff) {
            pickupMethod = "CODE";
        } else if (!ExpressConstants.PICKUP_METHOD_SET.contains(pickupMethod)) {
            return R.error("取件方式不正确，只能为 CODE、SCAN、ADMIN 或 COURIER！");
        }
        // 3. 取件记录前置查重（与 uk_express_id 同口径）
        if (pickupRecordMapper.countByExpressId(vo.getId()) > 0) {
            return R.error("该快递已有取件记录，不可重复取件！");
        }
        // 4. 当前用户；取件人 = 收件用户，未关联时记当前登录人
        String currentId = SecurityUtils.getCurrentUserId();
        if (!StringUtils.hasText(currentId)) {
            return R.error("未获取到当前用户信息！");
        }
        Long operatorId = Long.valueOf(currentId);
        Long receiverUserId = vo.getReceiverUserId() != null ? vo.getReceiverUserId() : operatorId;
        // 5. 货位写前预读（拿到所属货架；数据缺失提前返回业务错误）
        Long shelfId = null;
        if (vo.getLocationId() != null) {
            ShelfLocationVO location = shelfLocationMapper.selectById(vo.getLocationId());
            if (location == null) {
                return R.error("货位数据异常，请联系管理员！");
            }
            shelfId = location.getShelfId();
        }
        LocalDateTime now = LocalDateTime.now();
        // 6. 取件出库（首个写操作，状态守卫防并发重复取件；未通过时无任何变更）
        int rows = expressMapper.updatePickup(vo.getId(), ExpressConstants.STATUS_PICKED_UP, now,
                ExpressConstants.STATUS_WAITING_PICKUP, now);
        if (rows == 0) {
            return R.error("该快递状态已变更，请刷新后重试！");
        }
        // 7. 释放货位并自愈刷新货架计数（used_count>0 守卫失败属数据不一致，抛异常回滚）
        if (vo.getLocationId() != null) {
            if (shelfLocationMapper.decreaseUsed(vo.getLocationId(), now) == 0) {
                throw new IllegalStateException("货位占用数据异常：释放失败，取件回滚");
            }
            shelfMapper.refreshUsed(shelfId, now);
        }
        // 8. 写入取件记录（uk_express_id 冲突抛 DuplicateKeyException 触发回滚）
        PickupRecord record = new PickupRecord();
        record.setExpressId(vo.getId());
        record.setUserId(receiverUserId);
        record.setPickupCode(vo.getPickupCode());
        record.setOperatorId(staff ? operatorId : null);
        record.setPickupMethod(pickupMethod);
        record.setPickupTime(now);
        pickupRecordMapper.insert(record);
        // 9. 取件成功通知（接收人 = 收件用户，未关联时通知当前登录人）
        notificationService.send(receiverUserId, vo.getId(), "取件成功通知",
                "您的快递（单号 " + vo.getExpressNo() + "）已于 " + CONTENT_TIME_FORMATTER.format(now)
                        + " 取件出库。",
                NotificationConstants.TYPE_PICKUP_SUCCESS);
        log.info("【取件】出库成功：expressId={}，expressNo={}，method={}",
                vo.getId(), vo.getExpressNo(), pickupMethod);
        expressCache.evictExpress(vo.getId(), vo.getExpressNo());
        return R.success("取件成功！");
    }

    @Override
    public R recordList(String kw, LocalDateTime startTime, LocalDateTime endTime,
                        Integer pageNum, Integer pageSize) {
        // 1. 分页参数与查询条件归一（PageHelper 默认第 1 页、每页 10 条）
        pageNum = pageNum == null ? 1 : pageNum;
        pageSize = pageSize == null ? 10 : pageSize;
        String kwTrim = StringUtils.hasText(kw) ? kw.trim() : null;
        // 2. 分页查询取件记录（单号/取件码/用户关键字 + 取件时间范围，按取件时间倒序）
        PageHelper.startPage(pageNum, pageSize);
        List<PickupRecordVO> list = pickupRecordMapper.selectRecordList(kwTrim, startTime, endTime);
        PageInfo<PickupRecordVO> pageInfo = new PageInfo<>(list);
        return R.success(list).add("total", pageInfo.getTotal());
    }

    /**
     * 加载并校验取件码对应的快递（状态与归属守卫）
     * @param code 取件码
     * @return 校验结果
     */
    private PickupCheck loadForPickup(String code) {
        if (!StringUtils.hasText(code)) {
            return new PickupCheck(null, "取件码不能为空！");
        }
        ExpressVO vo = expressMapper.selectByPickupCode(code.trim());
        if (vo == null) {
            return new PickupCheck(null, "取件码不正确或快递不存在！");
        }
        String statusError = checkStatus(vo);
        if (statusError != null) {
            return new PickupCheck(null, statusError);
        }
        String ownerError = checkOwnership(vo);
        if (ownerError != null) {
            return new PickupCheck(null, ownerError);
        }
        return new PickupCheck(vo, null);
    }

    /**
     * 状态守卫：仅待取件可正常取件，其余状态给出对应文案
     * @param vo 快递信息
     * @return 错误文案，待取件返回 null
     */
    private String checkStatus(ExpressVO vo) {
        String status = vo.getStatus();
        if (ExpressConstants.STATUS_WAITING_PICKUP.equals(status)) {
            return null;
        }
        if (ExpressConstants.STATUS_PICKED_UP.equals(status)) {
            return "该快递已取件！";
        }
        if (ExpressConstants.STATUS_EXCEPTION.equals(status)) {
            return "该快递为异常件，请联系工作人员处理！";
        }
        if (ExpressConstants.STATUS_RETURNED.equals(status)) {
            return "该快递已退回，无法取件！";
        }
        // WAITING / STORED：尚未入库上架
        return "该快递尚未入库上架，暂不可取件！";
    }

    /**
     * 归属守卫：普通用户仅可操作收件关联到本人的快递（未关联收件用户时凭取件码放行），工作人员放行
     * @param vo 快递信息
     * @return 错误文案，通过返回 null
     */
    private String checkOwnership(ExpressVO vo) {
        if (!RoleConstants.USER.equals(SecurityUtils.getCurrentRole())) {
            return null;
        }
        String currentId = SecurityUtils.getCurrentUserId();
        if (vo.getReceiverUserId() != null && !String.valueOf(vo.getReceiverUserId()).equals(currentId)) {
            return "无权查看该快递！";
        }
        return null;
    }
}
