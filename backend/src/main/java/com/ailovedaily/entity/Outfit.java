package com.ailovedaily.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 穿搭方案实体类
 */
@Data
@TableName("outfit")
public class Outfit {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private Long coupleId;

    private String title;

    private String occasion;

    private String prompt;

    private String aiGeneratedImageUrl;

    /** JSON 数组字符串，如 [12,15,21] */
    private String itemIds;

    private String reason;

    @TableLogic
    private Integer deleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
