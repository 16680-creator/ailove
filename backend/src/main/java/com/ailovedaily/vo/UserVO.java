package com.ailovedaily.vo;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 用户信息VO
 */
@Data
public class UserVO {

    private Long id;

    private String nickname;

    private String avatarUrl;

    private Integer gender;

    private String phone;

    private LocalDate birthday;

    private Long coupleId;

    private Integer role;

    private LocalDateTime createTime;

    // 伴侣信息
    private UserVO partner;

    // 恋爱信息
    private LoveInfoVO loveInfo;
}
