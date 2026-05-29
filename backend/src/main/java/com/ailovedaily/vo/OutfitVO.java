package com.ailovedaily.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 穿搭方案VO
 */
@Data
public class OutfitVO {

    private Long id;

    private Long userId;

    private String title;

    private String occasion;

    private String prompt;

    private String aiGeneratedImageUrl;

    private String reason;

    private List<WardrobeItemVO> items;

    private LocalDateTime createTime;
}
