package com.ailovedaily.vo;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 生理期记录VO
 */
@Data
public class PeriodRecordVO {

    private Long id;

    private LocalDate startDate;

    private LocalDate endDate;

    private Integer cycleDays;

    private Integer periodDays;

    private String symptoms;

    private String mood;

    private Integer flowLevel;

    private String flowLevelText;

    private Integer painLevel;

    private String painLevelText;

    private String notes;

    private Boolean isPredicted;

    private LocalDateTime createTime;
}
