package com.ailovedaily.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 心愿清单实体类
 */
@Data
@TableName("wish_list")
public class WishList {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 情侣关系ID
     */
    private Long coupleId;

    /**
     * 创建人ID
     */
    private Long userId;

    /**
     * 心愿标题
     */
    private String title;

    /**
     * 心愿描述
     */
    private String description;

    /**
     * 心愿图片
     */
    private String imageUrl;

    /**
     * 分类: 1-旅行 2-美食 3-购物 4-体验 5-其他
     */
    private Integer category;

    /**
     * 优先级: 1-低 2-中 3-高 4-紧急
     */
    private Integer priority;

    /**
     * 状态: 0-待完成 1-进行中 2-已完成 3-已放弃
     */
    private Integer status;

    /**
     * 目标完成日期
     */
    private LocalDate targetDate;

    /**
     * 实际完成日期
     */
    private LocalDate completeDate;

    /**
     * 完成人ID
     */
    private Long completeBy;

    /**
     * 关联日记ID
     */
    private Long linkedDiaryId;

    /**
     * 关联照片ID数组(JSON)
     */
    private String linkedPhotoIds;

    /**
     * 排序
     */
    private Integer sortOrder;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
