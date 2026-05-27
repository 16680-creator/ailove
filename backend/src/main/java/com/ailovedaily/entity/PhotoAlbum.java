package com.ailovedaily.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 相册实体类
 */
@Data
@TableName("photo_album")
public class PhotoAlbum {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 情侣关系ID
     */
    private Long coupleId;

    /**
     * 相册名称
     */
    private String name;

    /**
     * 封面图片
     */
    private String coverUrl;

    /**
     * 描述
     */
    private String description;

    /**
     * 照片数量
     */
    private Integer photoCount;

    /**
     * 排序
     */
    private Integer sortOrder;

    /**
     * 创建人ID
     */
    private Long createBy;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
