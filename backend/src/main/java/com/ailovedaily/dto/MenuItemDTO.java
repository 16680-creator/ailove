package com.ailovedaily.dto;

import lombok.Data;

/**
 * 菜品DTO
 */
@Data
public class MenuItemDTO {

    private Long id;

    private String name;

    private String imageUrl;

    private Integer category;

    private String tags;

    private Integer difficulty;

    private Integer cookTime;

    private String description;
}
