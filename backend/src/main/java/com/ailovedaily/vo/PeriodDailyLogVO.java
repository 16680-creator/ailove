package com.ailovedaily.vo;

import lombok.Data;

import java.time.LocalDate;

/**
 * 经期每日打卡VO
 */
@Data
public class PeriodDailyLogVO {

    private Long id;

    private LocalDate logDate;

    private Integer isPeriod;

    private Integer flowLevel;

    private String flowLevelText;

    private Integer painLevel;

    private String painLevelText;

    private String symptoms;

    private String mood;

    private String notes;
}
