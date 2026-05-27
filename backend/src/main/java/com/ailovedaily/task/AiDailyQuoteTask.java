package com.ailovedaily.task;

import com.ailovedaily.service.AiQuoteService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * AI 每日情话定时任务
 * 每天早上 6:00 为所有情侣预生成今日 AI 情话
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AiDailyQuoteTask {

    private final AiQuoteService aiQuoteService;

    /**
     * 每天早上 6 点预生成
     */
    @Scheduled(cron = "0 0 6 * * ?")
    public void preGenerateDailyQuotes() {
        log.info("开始执行 AI 每日情话预生成任务");
        try {
            aiQuoteService.preGenerateForAllCouples();
            log.info("AI 每日情话预生成任务执行完成");
        } catch (Exception e) {
            log.error("AI 每日情话预生成任务执行失败", e);
        }
    }
}
