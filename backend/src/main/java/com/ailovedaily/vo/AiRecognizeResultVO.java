package com.ailovedaily.vo;

import lombok.Data;

import java.util.List;

/**
 * AI衣物识别结果VO
 */
@Data
public class AiRecognizeResultVO {

    private String category;

    private String subType;

    private String color;

    private String style;

    private List<String> season;

    private List<String> occasion;

    private List<String> tags;

    private Boolean success;
}
