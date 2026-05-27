package com.ailovedaily.service;

import com.ailovedaily.vo.AiQuoteVO;

/**
 * AI 情话服务接口
 */
public interface AiQuoteService {

    /**
     * 为指定情侣生成 AI 个性化情话
     *
     * @param coupleId 情侣关系ID
     * @param force    是否强制重新生成（忽略缓存）
     * @return AI 情话内容
     */
    AiQuoteVO generateQuote(Long coupleId, boolean force);

    /**
     * 为所有情侣预生成今日 AI 情话（定时任务调用）
     */
    void preGenerateForAllCouples();
}
