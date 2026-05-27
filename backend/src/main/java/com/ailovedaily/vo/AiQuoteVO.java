package com.ailovedaily.vo;

import lombok.Data;

/**
 * AI 每日情话 VO
 */
@Data
public class AiQuoteVO {

    /**
     * 情话内容
     */
    private String content;

    /**
     * 是否 AI 生成
     */
    private Boolean aiGenerated;

    /**
     * 生成时间
     */
    private String generateTime;
}
