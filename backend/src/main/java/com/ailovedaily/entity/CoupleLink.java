package com.ailovedaily.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 情侣关系实体类
 */
@Data
@TableName("couple_link")
public class CoupleLink {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 用户1ID
     */
    private Long user1Id;

    /**
     * 用户2ID
     */
    private Long user2Id;

    /**
     * 恋爱开始日期
     */
    private LocalDate loveStartDate;

    /**
     * 爱情宣言
     */
    private String loveMotto;

    /**
     * 合照URL
     */
    private String couplePhoto;

    /**
     * 邀请码
     */
    private String inviteCode;

    /**
     * 状态: 0-待绑定 1-已绑定 2-已解绑
     */
    private Integer status;

    /**
     * 绑定时间
     */
    private LocalDateTime bindTime;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
