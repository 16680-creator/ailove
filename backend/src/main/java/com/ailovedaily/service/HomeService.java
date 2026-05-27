package com.ailovedaily.service;

import com.ailovedaily.vo.HomeVO;

/**
 * 首页服务接口
 */
public interface HomeService {

    /**
     * 获取首页数据
     */
    HomeVO getHomeData(Long userId);

    /**
     * 获取每日一言
     */
    String getDailyQuote();

    /**
     * 获取 AI 个性化每日情话
     */
    com.ailovedaily.vo.AiQuoteVO getAiDailyQuote(Long userId, boolean force);
}
