package com.ailovedaily.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 衣物VO
 */
@Data
public class WardrobeItemVO {

    private Long id;

    private Long userId;

    private String imageUrl;

    private String thumbUrl;

    private String categoryCode;

    private String categoryName;

    private String subType;

    private String color;

    private String style;

    private List<String> season;

    private List<String> occasion;

    private List<String> tags;

    private Boolean aiRecognized;

    private Boolean favorite;

    private Integer wearCount;

    private LocalDateTime lastWearAt;

    private LocalDateTime createTime;
}
