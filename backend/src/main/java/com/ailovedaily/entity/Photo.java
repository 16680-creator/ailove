package com.ailovedaily.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 照片实体类
 */
@Data
@TableName("photo")
public class Photo {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 相册ID
     */
    private Long albumId;

    /**
     * 情侣关系ID
     */
    private Long coupleId;

    /**
     * 上传人ID
     */
    private Long userId;

    /**
     * 图片URL
     */
    private String url;

    /**
     * 缩略图URL
     */
    private String thumbnailUrl;

    /**
     * 描述
     */
    private String description;

    /**
     * 拍摄地点
     */
    private String location;

    /**
     * 拍摄时间
     */
    private LocalDateTime shootTime;

    /**
     * 文件大小(字节)
     */
    private Long fileSize;

    /**
     * 图片宽度
     */
    private Integer width;

    /**
     * 图片高度
     */
    private Integer height;

    /**
     * 是否收藏
     */
    @TableField("is_favorite")
    private Integer isFavorite;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
