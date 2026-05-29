package com.ailovedaily.dto;

import lombok.Data;

import java.util.List;

/**
 * 衣物更新DTO
 */
@Data
public class WardrobeItemUpdateDTO {

    private String categoryCode;

    private String subType;

    private String color;

    private String style;

    private List<String> season;

    private List<String> occasion;

    private List<String> tags;
}
