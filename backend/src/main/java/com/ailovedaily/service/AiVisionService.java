package com.ailovedaily.service;

import com.ailovedaily.vo.AiRecognizeResultVO;

/**
 * AI视觉识别服务接口
 */
public interface AiVisionService {

    /**
     * 识别衣物图片
     *
     * @param imageUrl 图片URL
     * @return 识别结果
     */
    AiRecognizeResultVO recognizeClothing(String imageUrl);
}
