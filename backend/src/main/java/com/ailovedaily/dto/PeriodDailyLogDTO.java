package com.ailovedaily.dto;

import lombok.Data;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

/**
 * 经期每日打卡DTO
 */
@Data
public class PeriodDailyLogDTO {

    @NotNull(message = "打卡日期不能为空")
    private LocalDate logDate;

    private Integer isPeriod;

    private Integer flowLevel;

    private Integer painLevel;

    private String symptoms;

    private String mood;

    private String notes;
}
