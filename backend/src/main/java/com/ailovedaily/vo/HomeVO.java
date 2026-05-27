package com.ailovedaily.vo;

import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 首页数据VO
 */
@Data
public class HomeVO {

    /**
     * 用户信息
     */
    private UserVO user;

    /**
     * 伴侣信息
     */
    private UserVO partner;

    /**
     * 恋爱信息
     */
    private LoveInfoVO loveInfo;

    /**
     * 每日一言
     */
    private String dailyQuote;

    /**
     * AI 情话信息
     */
    private AiQuoteVO aiQuote;

    /**
     * 快捷入口统计
     */
    private Map<String, Object> quickStats;

    /**
     * 最近日记
     */
    private List<DiaryVO> recentDiaries;

    /**
     * 最近照片
     */
    private List<PhotoVO> recentPhotos;
}
