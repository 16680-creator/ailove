package com.ailovedaily.service;

import com.ailovedaily.dto.MealRecordCreateDTO;
import com.ailovedaily.dto.MealReviewDTO;
import com.ailovedaily.vo.MealRecordVO;

import java.util.List;
import java.util.Map;

/**
 * 餐食记录服务接口
 */
public interface MealRecordService {

    /**
     * 创建餐食记录
     */
    void createMealRecord(Long userId, MealRecordCreateDTO dto);

    /**
     * 添加评价（每条记录限一次）
     */
    void addReview(Long userId, Long recordId, MealReviewDTO dto);

    /**
     * 获取最近N天的历史记录，按日期分组
     */
    Map<String, List<MealRecordVO>> getHistory(Long userId, Integer days);
}
