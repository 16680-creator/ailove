package com.ailovedaily.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 餐食记录实体类
 */
@Data
@TableName("meal_record")
public class MealRecord {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 情侣关系ID
     */
    private Long coupleId;

    /**
     * 下单人ID
     */
    private Long userId;

    /**
     * 用餐日期
     */
    private LocalDate mealDate;

    /**
     * 菜品快照JSON [{menuItemId,name,imageUrl,count}]
     */
    private String dishes;

    /**
     * 评分1-5星
     */
    private Integer rating;

    /**
     * 文字评价
     */
    private String comment;

    /**
     * 评价人ID
     */
    private Long reviewBy;

    /**
     * 评价时间
     */
    private LocalDateTime reviewTime;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
