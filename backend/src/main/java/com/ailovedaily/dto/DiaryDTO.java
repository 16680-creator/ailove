package com.ailovedaily.dto;

import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;

/**
 * 日记DTO
 */
@Data
public class DiaryDTO {

    private Long id;

    @NotBlank(message = "标题不能为空")
    @Size(max = 100, message = "标题长度不能超过100个字符")
    private String title;

    @Size(max = 5000, message = "内容长度不能超过5000个字符")
    private String content;

    private Integer mood;

    @Size(max = 20, message = "天气信息过长")
    private String weather;

    @Size(max = 100, message = "地点信息过长")
    private String location;

    @Size(max = 9, message = "图片数量不能超过9张")
    private List<String> images;

    private LocalDate diaryDate;
}
