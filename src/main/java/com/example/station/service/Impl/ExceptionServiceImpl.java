package com.example.station.service.Impl;

import com.example.station.common.R;
import com.example.station.annotation.constant.ExpressConstants;
import com.example.station.annotation.constant.NotificationConstants;
import com.example.station.annotation.constant.RoleConstants;
import com.example.station.dto.ExceptionDTO;
import com.example.station.dto.ExceptionHandleDTO;
import com.example.station.entity.ExpressException;
import com.example.station.mapper.ExpressExceptionMapper;
import com.example.station.mapper.ExpressMapper;
import com.example.station.mapper.SysUserMapper;
import com.example.station.service.ExceptionService;
import com.example.station.service.NotificationService;
import com.example.station.utils.SecurityUtils;
import com.example.station.vo.ExceptionVO;
import com.example.station.vo.ExpressVO;
import com.example.station.vo.UserVO;
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
 * 异常件业务实现
 * 写序规则：全部业务校验在前；异常记录写入后，快递状态更新与通知失败一律抛异常触发事务回滚
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ExceptionServiceImpl implements ExceptionService {

    private final ExpressExceptionMapper expressExceptionMapper;
    private final ExpressMapper expressMapper;
    private final SysUserMapper sysUserMapper;
    private final NotificationService notificationService;

    @Override
    public R list(Long expressId, String exceptionType, String handleStatus, Long reporterId, String keyword,
                  Integer pageNum, Integer pageSize) {
        // 1. 分页参数与筛选条件归一（类型/状态非法时给出与详情一致的枚举文案）
        pageNum = pageNum == null ? 1 : pageNum;
        pageSize = pageSize == null ? 10 : pageSize;
        String typeTrim = StringUtils.hasText(exceptionType) ? exceptionType.trim() : null;
        if (typeTrim != null && !ExpressConstants.EXCEPTION_TYPE_SET.contains(typeTrim)) {
            return R.error(typeErrorMsg());
        }
        String statusTrim = StringUtils.hasText(handleStatus) ? handleStatus.trim() : null;
        if (statusTrim != null && !ExpressConstants.HANDLE_STATUS_SET.contains(statusTrim)) {
            return R.error(handleStatusErrorMsg());
        }
        String keywordTrim = StringUtils.hasText(keyword) ? keyword.trim() : null;
        // 2. 分页查询异常件
        PageHelper.startPage(pageNum, pageSize);
        List<ExceptionVO> list = expressExceptionMapper.selectList(expressId, typeTrim, statusTrim,
                reporterId, keywordTrim);
        PageInfo<ExceptionVO> pageInfo = new PageInfo<>(list);
        return R.success(list).add("total", pageInfo.getTotal());
    }

    @Override
    public R detail(Long id) {
        if (id == null) {
            return R.error("异常记录ID不能为空！");
        }
        ExceptionVO vo = expressExceptionMapper.selectById(id);
        if (vo == null) {
            return R.error("异常记录不存在！");
        }
        return R.success(vo);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public R add(ExceptionDTO dto) {
        // 1. 兜底与枚举校验（expressId 与 expressNo 二选一必填）
        if (dto == null || (dto.getExpressId() == null && !StringUtils.hasText(dto.getExpressNo()))) {
            return R.error("请提供快递单号或快递ID！");
        }
        if (!StringUtils.hasText(dto.getExceptionType())
                || !ExpressConstants.EXCEPTION_TYPE_SET.contains(dto.getExceptionType().trim())) {
            return R.error(typeErrorMsg());
        }
        if (!StringUtils.hasText(dto.getDescription())) {
            return R.error("异常描述不能为空！");
        }
        // 2. 快递定位与重复登记校验（优先快递ID，缺省按单号精确匹配：按单号登记免去换查快递列表的权限依赖）
        ExpressVO express = dto.getExpressId() != null
                ? expressMapper.selectById(dto.getExpressId())
                : expressMapper.selectByExpressNo(dto.getExpressNo().trim());
        if (express == null) {
            return R.error("快递不存在！");
        }
        if (ExpressConstants.STATUS_EXCEPTION.equals(express.getStatus())) {
            return R.error("该快递已登记异常，不可重复登记！");
        }
        // 3. 当前登录人（登记人）
        String currentId = SecurityUtils.getCurrentUserId();
        if (!StringUtils.hasText(currentId)) {
            return R.error("未获取到当前用户信息！");
        }
        Long reporterId = Long.valueOf(currentId);
        // 4. 普通用户归属校验：收件用户关联到本人，或本人手机号命中收/寄件电话
        String ownerError = checkOwnership(express, reporterId);
        if (ownerError != null) {
            return R.error(ownerError);
        }
        // 5. 写入异常记录（首个写操作：后续失败一律抛异常回滚）
        ExpressException record = new ExpressException();
        record.setExpressId(express.getId());
        record.setExceptionType(dto.getExceptionType().trim());
        record.setDescription(dto.getDescription().trim());
        record.setImageUrl(StringUtils.hasText(dto.getImageUrl()) ? dto.getImageUrl().trim() : null);
        record.setReporterId(reporterId);
        record.setHandleStatus(ExpressConstants.HANDLE_STATUS_PENDING);
        expressExceptionMapper.insert(record);
        // 6. 快件处于在库环节时状态置为异常（已取件/已退回的收尾问题只记录不改状态）
        if (ExpressConstants.STATUS_WAITING.equals(express.getStatus())
                || ExpressConstants.STATUS_STORED.equals(express.getStatus())
                || ExpressConstants.STATUS_WAITING_PICKUP.equals(express.getStatus())) {
            expressMapper.updateStatus(express.getId(), ExpressConstants.STATUS_EXCEPTION, LocalDateTime.now());
        }
        // 7. 通知全部管理员（随事务提交）
        String type = ExpressConstants.EXCEPTION_TYPE_SET.contains(dto.getExceptionType().trim())
                ? dto.getExceptionType().trim() : "OTHER";
        notificationService.sendToAdmins(express.getId(), "异常件登记提醒",
                "快递（单号 " + express.getExpressNo() + "）登记异常，类型：" + type
                        + "，描述：" + dto.getDescription().trim() + "，请及时处理。",
                NotificationConstants.TYPE_EXCEPTION_NOTICE);
        log.info("【异常】登记成功：expressId={}，reporterId={}，type={}", express.getId(), reporterId, type);
        return R.success("异常登记成功！");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public R handle(ExceptionHandleDTO dto) {
        // 1. 兜底与枚举校验
        if (dto == null || dto.getId() == null) {
            return R.error("异常记录ID不能为空！");
        }
        if (!StringUtils.hasText(dto.getHandleStatus())
                || !ExpressConstants.HANDLE_STATUS_SET.contains(dto.getHandleStatus().trim())) {
            return R.error(handleStatusErrorMsg());
        }
        String status = dto.getHandleStatus().trim();
        String remark = StringUtils.hasText(dto.getHandleRemark()) ? dto.getHandleRemark().trim() : null;
        boolean terminal = ExpressConstants.HANDLE_STATUS_TERMINAL.contains(status);
        if (terminal && remark == null) {
            return R.error("请填写处理结果！");
        }
        String currentId = SecurityUtils.getCurrentUserId();
        if (!StringUtils.hasText(currentId)) {
            return R.error("未获取到当前用户信息！");
        }
        Long handlerId = Long.valueOf(currentId);
        // 2. 记录存在性校验（首个写操作前的最后校验）
        ExceptionVO current = expressExceptionMapper.selectById(dto.getId());
        if (current == null) {
            return R.error("异常记录不存在！");
        }
        LocalDateTime now = LocalDateTime.now();
        // 3. 更新处理状态与结果（首个写操作）
        expressExceptionMapper.updateHandle(dto.getId(), handlerId, status, remark, now, now);
        // 4. 终态：恢复快递状态（仍处于异常态时才恢复，防覆盖后续人工修正）并通知收件用户
        if (terminal) {
            ExpressVO express = expressMapper.selectById(current.getExpressId());
            if (express != null && ExpressConstants.STATUS_EXCEPTION.equals(express.getStatus())) {
                String restored = express.getLocationId() != null
                        ? ExpressConstants.STATUS_WAITING_PICKUP : ExpressConstants.STATUS_WAITING;
                expressMapper.updateStatus(express.getId(), restored, now);
            }
            if (express != null && express.getReceiverUserId() != null) {
                notificationService.send(express.getReceiverUserId(), express.getId(), "异常件处理完成",
                        "您的快递（单号 " + express.getExpressNo() + "）异常已处理，处理结果：" + remark + "。",
                        NotificationConstants.TYPE_EXCEPTION_NOTICE);
            }
        }
        log.info("【异常】处理成功：id={}，handlerId={}，status={}", dto.getId(), handlerId, status);
        return R.success("处理成功！");
    }

    /**
     * 普通用户登记归属校验：收件用户关联到本人，或本人手机号命中收/寄件电话
     * @param express 快递信息
     * @param reporterId 当前登录人ID
     * @return 错误文案，通过返回 null（工作人员放行）
     */
    private String checkOwnership(ExpressVO express, Long reporterId) {
        // 工作人员（管理员/快递员）放行，仅普通用户做归属校验
        if (!RoleConstants.USER.equals(SecurityUtils.getCurrentRole())) {
            return null;
        }
        if (express.getReceiverUserId() != null && express.getReceiverUserId().equals(reporterId)) {
            return null;
        }
        UserVO me = sysUserMapper.selectById(reporterId);
        String phone = me != null ? me.getPhone() : null;
        if (StringUtils.hasText(phone)
                && (phone.equals(express.getReceiverPhone()) || phone.equals(express.getSenderPhone()))) {
            return null;
        }
        return "无权操作该快递！";
    }

    /**
     * 异常类型枚举错误文案（与新增入参校验同源）
     * @return 错误文案
     */
    private String typeErrorMsg() {
        return "异常类型不正确，只能为 DAMAGE、LOST、WRONG_ADDR、WRONG_INFO、REFUSED、OVERDUE 或 OTHER！";
    }

    /**
     * 处理状态枚举错误文案
     * @return 错误文案
     */
    private String handleStatusErrorMsg() {
        return "处理状态不正确，只能为 PENDING、PROCESSING、RESOLVED、CLOSED 或 REJECTED！";
    }
}
