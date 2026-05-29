package com.ailovedaily.service;

import java.util.List;

/**
 * AI图像生成服务接口
 */
public interface AiImageService {

    /**
     * 生成穿搭效果图
     *
     * @param prompt           提示词
     * @param referenceImages  参考图片URL列表
     * @return 本地保存后的图片URL
     */
    String generateOutfitImage(String prompt, List<String> referenceImages);
}
