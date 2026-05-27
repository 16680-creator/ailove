package com.ailovedaily.service;

import com.ailovedaily.dto.PeriodDailyLogDTO;
import com.ailovedaily.dto.PeriodRecordDTO;
import com.ailovedaily.vo.PeriodDailyLogVO;
import com.ailovedaily.vo.PeriodInfoVO;
import com.ailovedaily.vo.PeriodRecordVO;

import java.time.LocalDate;
import java.util.List;

/**
 * 生理期服务接口
 */
public interface PeriodService {

    /**
     * 记录生理期
     */
    void recordPeriod(Long userId, PeriodRecordDTO recordDTO);

    /**
     * 更新生理期记录
     */
    void updatePeriod(Long id, Long userId, PeriodRecordDTO recordDTO);

    /**
     * 删除生理期记录
     */
    void deletePeriod(Long id, Long userId);

    /**
     * 获取生理期信息（包含预测）
     */
    PeriodInfoVO getPeriodInfo(Long userId);

    /**
     * 获取最近记录
     */
    List<PeriodRecordVO> getRecentRecords(Long userId, Integer limit);

    /**
     * 生成预测记录
     */
    void generatePredictions(Long userId);

    /**
     * 检查并发送提醒
     */
    void checkAndSendReminders();

    /**
     * 保存/更新每日打卡
     */
    void saveDailyLog(Long userId, PeriodDailyLogDTO dto);

    /**
     * 获取月度日志
     */
    List<PeriodDailyLogVO> getMonthlyLogs(Long userId, int year, int month);

    /**
     * 获取某天日志
     */
    PeriodDailyLogVO getDailyLog(Long userId, LocalDate date);
}
