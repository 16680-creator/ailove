package com.ailovedaily.dto;

import lombok.Data;

import java.time.LocalDate;
import java.util.List;

/**
 * 心愿清单DTO
 */
@Data
public class WishListDTO {

    private Long id;

    private String title;

    private String description;

    private String imageUrl;

    private Integer category;

    private Integer priority;

    private Integer status;

    private LocalDate targetDate;

    private Long linkedDiaryId;

    private List<Long> linkedPhotoIds;
}
