package com.ailovedaily.task;

import com.ailovedaily.service.PeriodService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 生理期提醒定时任务
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PeriodReminderTask {

    private final PeriodService periodService;

    /**
     * 每天早上8点检查并发送提醒
     */
    @Scheduled(cron = "0 0 8 * * ?")
    public void checkAndSendReminders() {
        log.info("开始执行生理期提醒任务");
        try {
            periodService.checkAndSendReminders();
            log.info("生理期提醒任务执行完成");
        } catch (Exception e) {
            log.error("生理期提醒任务执行失败", e);
        }
    }
}
