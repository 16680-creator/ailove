package com.ailovedaily.service;

import com.ailovedaily.dto.TripPlanDTO;
import com.ailovedaily.entity.TripPlan;

import java.util.List;

public interface TripPlanService {

    /**
     * 启动生成（异步），返回行程记录 ID
     */
    Long startGenerate(TripPlanDTO dto, Long userId);

    /**
     * 查询单条行程
     */
    TripPlan getById(Long id);

    /**
     * 查询用户的行程列表
     */
    List<TripPlan> getUserPlans(Long userId);

    /**
     * 删除行程
     */
    void delete(Long id, Long userId);
}
