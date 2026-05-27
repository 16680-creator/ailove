package com.ailovedaily.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

/**
 * 旅行行程规划 DTO
 */
@Data
public class TripPlanDTO {

    @NotBlank(message = "出发城市不能为空")
    @Size(max = 50, message = "城市名过长")
    private String fromCity;

    @NotBlank(message = "目的地不能为空")
    @Size(max = 50, message = "城市名过长")
    private String toCity;

    @NotBlank(message = "出发日期不能为空")
    private String startDate;

    @NotBlank(message = "返回日期不能为空")
    private String endDate;

    /**
     * 偏好：美食/文艺/自然/历史/购物，逗号分隔
     */
    @Size(max = 100, message = "偏好信息过长")
    private String preferences;

    /**
     * 预算等级：经济/适中/宽松
     */
    private String budget;

    /**
     * 自定义需求
     */
    private String customRequest;
}
