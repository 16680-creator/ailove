package com.ailovedaily.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 菜品实体类
 */
@Data
@TableName("menu_item")
public class MenuItem {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 情侣关系ID
     */
    private Long coupleId;

    /**
     * 菜品名称
     */
    private String name;

    /**
     * 图片URL
     */
    private String imageUrl;

    /**
     * 分类: 1-家常菜 2-西餐 3-小吃 4-甜品 5-饮品
     */
    private Integer category;

    /**
     * 标签,逗号分隔
     */
    private String tags;

    /**
     * 难度: 1-5星
     */
    private Integer difficulty;

    /**
     * 烹饪时间(分钟)
     */
    private Integer cookTime;

    /**
     * 描述
     */
    private String description;

    /**
     * AI生成的做法
     */
    private String recipe;

    /**
     * 创建人ID
     */
    private Long createBy;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
