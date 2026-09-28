package com.example.station.task;

import com.example.station.service.OverdueService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 超期检测定时任务
 * 每小时执行一次超期标记与通知（与手动 /overdue/refresh 共用同一实现）；
 * 失败仅记录错误日志，不中断调度，下一轮自动补偿
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OverdueTask {

    /** 超期检测业务（定时与手动共用入口） */
    private final OverdueService overdueService;

    /**
     * 每小时整点执行超期检测
     */
    @Scheduled(cron = "0 0 * * * *")
    public void checkOverdue() {
        try {
            int count = overdueService.checkOverdue();
            if (count > 0) {
                log.info("【超期】定时检测完成：新标记 {} 件", count);
            }
        } catch (Exception e) {
            log.error("【超期】定时检测失败：{}", e.getMessage());
        }
    }
}
