package com.ailovedaily.dto;

import lombok.Data;

import java.time.LocalDate;

/**
 * 情侣绑定DTO
 */
@Data
public class CoupleBindDTO {

    /**
     * 邀请码
     */
    private String inviteCode;

    /**
     * 恋爱开始日期
     */
    private LocalDate loveStartDate;

    /**
     * 爱情宣言
     */
    private String loveMotto;
}
