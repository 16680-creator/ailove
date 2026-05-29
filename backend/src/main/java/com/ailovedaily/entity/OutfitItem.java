package com.ailovedaily.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 穿搭方案衣物关联实体类
 */
@Data
@TableName("outfit_item")
public class OutfitItem {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long outfitId;

    private Long wardrobeItemId;

    private String slot;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
