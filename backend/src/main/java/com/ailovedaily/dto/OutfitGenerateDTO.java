package com.ailovedaily.dto;

import lombok.Data;

import java.util.List;

/**
 * 穿搭生成DTO
 */
@Data
public class OutfitGenerateDTO {

    private String prompt;

    private List<Long> itemIds;

    private Boolean partnerView;
}
