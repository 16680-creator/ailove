package com.ailovedaily.dto;

import lombok.Data;

/**
 * 餐食评价DTO
 */
@Data
public class MealReviewDTO {

    /**
     * 评分1-5星
     */
    private Integer rating;

    /**
     * 文字评价
     */
    private String comment;
}
