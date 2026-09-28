package com.example.station.service.Impl;

import com.example.station.common.R;
import com.example.station.annotation.constant.ConfigConstants;
import com.example.station.annotation.constant.NotificationConstants;
import com.example.station.mapper.ExpressMapper;
import com.example.station.service.NotificationService;
import com.example.station.service.OverdueService;
import com.example.station.service.SysConfigService;
import com.example.station.vo.ExpressVO;
import com.example.station.vo.OverdueVO;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 超期管理业务实现
 * 标记守卫：UPDATE ... WHERE overdue_flag=0 保证同一条快递只被标记、只发一次通知
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OverdueServiceImpl implements OverdueService {

    private final ExpressMapper expressMapper;
    private final NotificationService notificationService;
    private final SysConfigService sysConfigService;

    @Override
    public R list(String keyword, Integer pageNum, Integer pageSize) {
        // 1. 分页参数与查询条件归一
        pageNum = pageNum == null ? 1 : pageNum;
        pageSize = pageSize == null ? 10 : pageSize;
        String keywordTrim = StringUtils.hasText(keyword) ? keyword.trim() : null;
        int hours = overdueHours();
        // 2. 分页查询超期快递（超期时长按配置时长实时计算）
        PageHelper.startPage(pageNum, pageSize);
        List<OverdueVO> list = expressMapper.selectOverdueList(keywordTrim, hours);
        PageInfo<OverdueVO> pageInfo = new PageInfo<>(list);
        return R.success(list).add("total", pageInfo.getTotal());
    }

    @Override
    public R refresh() {
        int count = checkOverdue();
        log.info("【超期】手动检测完成：本次新标记 {} 件", count);
        return R.success("超期检测完成，本次标记 " + count + " 件！");
    }

    @Override
    public R remind(Long id) {
        // 1. 快递存在且处于超期状态
        if (id == null) {
            return R.error("快递ID不能为空！");
        }
        ExpressVO vo = expressMapper.selectById(id);
        if (vo == null) {
            return R.error("快递不存在！");
        }
        if (vo.getOverdueFlag() == null || vo.getOverdueFlag() != 1) {
            return R.error("该快递未超期，无需提醒！");
        }
        // 2. 关联收件用户后发送提醒
        if (vo.getReceiverUserId() == null) {
            return R.error("该快递未关联收件用户，无法发送提醒！");
        }
        notificationService.send(vo.getReceiverUserId(), id, "快递超期提醒",
                "您的快递（单号 " + vo.getExpressNo() + "）仍未取件，入库时间：" + vo.getStorageTime()
                        + "，已超过约定保管时长，请尽快取件。",
                NotificationConstants.TYPE_OVERDUE_NOTICE);
        log.info("【超期】发送提醒：expressId={}，receiverUserId={}", id, vo.getReceiverUserId());
        return R.success("提醒发送成功！");
    }

    @Override
    public int checkOverdue() {
        int hours = overdueHours();
        LocalDateTime cutoff = LocalDateTime.now().minusHours(hours);
        List<ExpressVO> candidates = expressMapper.selectOverdueCandidates(cutoff);
        int count = 0;
        for (ExpressVO candidate : candidates) {
            // 行级守卫：并发/重复执行时仅首次标记返回 1 行，通知也只发一次
            int rows = expressMapper.updateOverdueFlag(candidate.getId(), LocalDateTime.now());
            if (rows == 0) {
                continue;
            }
            count++;
            // 1) 收件用户（未关联收件用户时 send 内部跳过）
            notificationService.send(candidate.getReceiverUserId(), candidate.getId(), "快递超期通知",
                    "您的快递（单号 " + candidate.getExpressNo() + "）已超期（入库时间："
                            + candidate.getStorageTime() + "，超期阈值 " + hours + " 小时），请尽快取件。",
                    NotificationConstants.TYPE_OVERDUE_NOTICE);
            // 2) 全部管理员
            notificationService.sendToAdmins(candidate.getId(), "超期快递提醒",
                    "快递（单号 " + candidate.getExpressNo() + "）已超过 " + hours + " 小时未取，收件人："
                            + candidate.getReceiverName() + "（" + candidate.getReceiverPhone() + "），请跟进处理。",
                    NotificationConstants.TYPE_OVERDUE_NOTICE);
        }
        if (count > 0) {
            log.info("【超期】检测完成：新标记 {} 件，阈值 {} 小时", count, hours);
        }
        return count;
    }

    /**
     * 读取超期时长配置（缺失/非法回退默认值）
     * @return 超期小时数
     */
    private int overdueHours() {
        return sysConfigService.getIntValue(ConfigConstants.KEY_OVERDUE_HOURS,
                ConfigConstants.DEFAULT_OVERDUE_HOURS);
    }
}
