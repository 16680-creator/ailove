package com.ailovedaily.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 旅行行程实体类
 */
@Data
@TableName("trip_plan")
public class TripPlan {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private Long coupleId;

    private String fromCity;

    private String toCity;

    private String startDate;

    private String endDate;

    private String preferences;

    private String budget;

    private String customRequest;

    /**
     * 状态: 0=生成中 1=已完成 2=失败
     */
    private Integer status;

    /**
     * AI 生成的行程 JSON
     */
    private String resultJson;

    private String errorMsg;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
