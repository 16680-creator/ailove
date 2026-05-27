package com.ailovedaily.vo;

import lombok.Data;

import java.time.LocalDate;

/**
 * 恋爱信息VO
 */
@Data
public class LoveInfoVO {

    /**
     * 在一起天数
     */
    private Long daysTogether;

    /**
     * 恋爱开始日期
     */
    private LocalDate loveStartDate;

    /**
     * 爱情宣言
     */
    private String loveMotto;

    /**
     * 下一纪念日天数
     */
    private Long nextAnniversaryDays;

    /**
     * 合照URL
     */
    private String couplePhoto;
}
