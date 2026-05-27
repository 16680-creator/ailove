package com.ailovedaily.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 每日一言实体类
 */
@Data
@TableName("daily_quote")
public class DailyQuote {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 内容
     */
    private String content;

    /**
     * 作者
     */
    private String author;

    /**
     * 分类: 1-情话 2-励志 3-幽默
     */
    private Integer category;

    /**
     * 使用次数
     */
    private Integer useCount;

    /**
     * 来源: 0-人工录入 1-AI生成
     */
    private Integer source;

    /**
     * 关联情侣ID(AI个性化生成时)
     */
    private Long coupleId;

    /**
     * 生效日期(AI情话按天缓存)
     */
    private LocalDate quoteDate;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
