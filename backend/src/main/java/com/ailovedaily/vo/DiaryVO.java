package com.ailovedaily.vo;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 日记VO
 */
@Data
public class DiaryVO {

    private Long id;

    private Long userId;

    private String userNickname;

    private String userAvatar;

    private String title;

    private String content;

    private Integer mood;

    private String moodText;

    private String weather;

    private String location;

    private List<String> images;

    private Integer isFavorite;

    private Integer viewCount;

    private LocalDate diaryDate;

    private LocalDateTime createTime;

    // 是否当前用户发布
    private Boolean isMine;
}
