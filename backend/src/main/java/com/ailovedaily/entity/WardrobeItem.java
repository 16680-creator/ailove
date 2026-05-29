package com.ailovedaily.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 衣物实体类
 */
@Data
@TableName("wardrobe_item")
public class WardrobeItem {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private Long coupleId;

    private String imageUrl;

    private String thumbUrl;

    private String categoryCode;

    private String subType;

    private String color;

    private String style;

    /** JSON 数组字符串，如 ["spring","summer"] */
    private String season;

    /** JSON 数组字符串，如 ["daily","date"] */
    private String occasion;

    /** JSON 数组字符串 */
    private String aiTags;

    private Integer aiRecognized;

    private Integer favorite;

    private Integer wearCount;

    private LocalDateTime lastWearAt;

    @TableLogic
    private Integer deleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
