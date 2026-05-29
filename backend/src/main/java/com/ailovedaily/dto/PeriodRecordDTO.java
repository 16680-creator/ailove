package com.ailovedaily.dto;

import lombok.Data;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

/**
 * 生理期记录DTO
 */
@Data
public class PeriodRecordDTO {

    private Long id;

    @NotNull(message = "开始日期不能为空")
    private LocalDate startDate;

    private LocalDate endDate;

    private Integer cycleDays;

    private Integer periodDays;

    private String symptoms;

    private String mood;

    private Integer flowLevel;

    private Integer painLevel;

    private String notes;
}
