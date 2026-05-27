package com.ailovedaily.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 照片VO
 */
@Data
public class PhotoVO {

    private Long id;

    private Long albumId;

    private String albumName;

    private Long userId;

    private String userNickname;

    private String url;

    private String thumbnailUrl;

    private String description;

    private String location;

    private LocalDateTime shootTime;

    private Integer width;

    private Integer height;

    private Integer isFavorite;

    private LocalDateTime createTime;
}
