package com.ailovedaily.service;

/**
 * AI 菜品做法服务
 */
public interface DishRecipeService {

    /**
     * 为指定菜品生成 AI 做法（同步）
     *
     * @param menuItemId 菜品ID
     * @return 生成的做法文本，失败返回 null
     */
    String generateRecipe(Long menuItemId);

    /**
     * 异步生成菜品做法（不阻塞调用方）
     *
     * @param menuItemId 菜品ID
     */
    void generateRecipeAsync(Long menuItemId);
}
