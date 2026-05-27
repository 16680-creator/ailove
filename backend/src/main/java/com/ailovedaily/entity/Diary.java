package com.ailovedaily.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 日记实体类
 */
@Data
@TableName("diary")
public class Diary {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 情侣关系ID
     */
    private Long coupleId;

    /**
     * 作者ID
     */
    private Long userId;

    /**
     * 标题
     */
    private String title;

    /**
     * 内容
     */
    private String content;

    /**
     * 心情: 1-开心 2-感动 3-平静 4-难过 5-生气
     */
    private Integer mood;

    /**
     * 天气
     */
    private String weather;

    /**
     * 地点
     */
    private String location;

    /**
     * 图片数组(JSON)
     */
    private String images;

    /**
     * 是否收藏: 0-否 1-是
     */
    @TableField("is_favorite")
    private Integer isFavorite;

    /**
     * 浏览次数
     */
    private Integer viewCount;

    /**
     * 日记日期
     */
    private LocalDate diaryDate;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
