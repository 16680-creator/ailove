package com.ailovedaily.vo;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 餐食记录VO
 */
@Data
public class MealRecordVO {

    private Long id;

    private Long userId;

    private String userNickname;

    private LocalDate mealDate;

    /**
     * 菜品列表 [{menuItemId,name,imageUrl,count}]
     */
    private List<DishSnapshot> dishes;

    /**
     * 评分1-5星
     */
    private Integer rating;

    /**
     * 文字评价
     */
    private String comment;

    /**
     * 评价人昵称
     */
    private String reviewByName;

    private LocalDateTime reviewTime;

    private LocalDateTime createTime;

    /**
     * 是否可评价（rating==null）
     */
    private Boolean canReview;

    @Data
    public static class DishSnapshot {
        private Long menuItemId;
        private String name;
        private String imageUrl;
        private Integer count;
    }
}
