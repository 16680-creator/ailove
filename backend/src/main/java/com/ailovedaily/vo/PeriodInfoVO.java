package com.ailovedaily.vo;

import lombok.Data;

import java.time.LocalDate;
import java.util.List;

/**
 * 生理期信息VO
 */
@Data
public class PeriodInfoVO {

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 用户昵称
     */
    private String userNickname;

    /**
     * 平均周期天数
     */
    private Integer avgCycleDays;

    /**
     * 平均经期天数
     */
    private Integer avgPeriodDays;

    /**
     * 下次预计开始日期
     */
    private LocalDate nextPredictedDate;

    /**
     * 距离下次还有几天
     */
    private Long daysUntilNext;

    /**
     * 当前状态: 0-安全期 1-易孕期 2-经期
     */
    private Integer currentStatus;

    /**
     * 当前状态文本
     */
    private String currentStatusText;

    /**
     * 最近记录
     */
    private List<PeriodRecordVO> recentRecords;

    /**
     * 是否需要提醒
     */
    private Boolean needRemind;
}
