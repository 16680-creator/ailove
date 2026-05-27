package com.ailovedaily.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 经期每日打卡记录实体类
 */
@Data
@TableName("period_daily_log")
public class PeriodDailyLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private Long coupleId;

    private LocalDate logDate;

    /**
     * 0=非经期 1=经期
     */
    @TableField("is_period")
    private Integer isPeriod;

    /**
     * 流量: 1=少 2=中 3=多
     */
    private Integer flowLevel;

    /**
     * 疼痛: 0=无 1=轻 2=中 3=重
     */
    private Integer painLevel;

    private String symptoms;

    private String mood;

    private String notes;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
