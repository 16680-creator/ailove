package com.ailovedaily.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 生理期记录实体类
 */
@Data
@TableName("period_record")
public class PeriodRecord {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 用户ID(女生)
     */
    private Long userId;

    /**
     * 情侣关系ID
     */
    private Long coupleId;

    /**
     * 开始日期
     */
    private LocalDate startDate;

    /**
     * 结束日期
     */
    private LocalDate endDate;

    /**
     * 周期天数
     */
    private Integer cycleDays;

    /**
     * 经期天数
     */
    private Integer periodDays;

    /**
     * 症状,逗号分隔
     */
    private String symptoms;

    /**
     * 心情
     */
    private String mood;

    /**
     * 流量: 1-少 2-中 3-多
     */
    private Integer flowLevel;

    /**
     * 疼痛: 0-无 1-轻 2-中 3-重
     */
    private Integer painLevel;

    /**
     * 备注
     */
    private String notes;

    /**
     * 是否预测数据: 0-实际 1-预测
     */
    @TableField("is_predicted")
    private Integer isPredicted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
