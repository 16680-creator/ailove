package com.ailovedaily.dto;

import lombok.Data;

import java.util.List;

/**
 * 创建餐食记录DTO
 */
@Data
public class MealRecordCreateDTO {

    /**
     * 菜品列表
     */
    private List<DishItem> dishes;

    @Data
    public static class DishItem {
        /**
         * 菜品ID
         */
        private Long menuItemId;
        /**
         * 数量
         */
        private Integer count;
    }
}
