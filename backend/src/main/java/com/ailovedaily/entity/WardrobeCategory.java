package com.ailovedaily.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 衣物分类字典实体类
 */
@Data
@TableName("wardrobe_category")
public class WardrobeCategory {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String code;

    private String name;

    private Integer sortOrder;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
