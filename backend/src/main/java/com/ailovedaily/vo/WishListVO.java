package com.ailovedaily.vo;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 心愿清单VO
 */
@Data
public class WishListVO {

    private Long id;

    private Long userId;

    private String userNickname;

    private String title;

    private String description;

    private String imageUrl;

    private Integer category;

    private String categoryText;

    private Integer priority;

    private String priorityText;

    private Integer status;

    private String statusText;

    private LocalDate targetDate;

    private LocalDate completeDate;

    private Long completeBy;

    private String completeByNickname;

    private Long linkedDiaryId;

    private List<Long> linkedPhotoIds;

    private LocalDateTime createTime;
}
