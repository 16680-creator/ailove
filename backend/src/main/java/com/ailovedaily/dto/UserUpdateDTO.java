package com.ailovedaily.dto;

import lombok.Data;

import jakarta.validation.constraints.Size;
import java.time.LocalDate;

/**
 * 用户信息更新DTO
 */
@Data
public class UserUpdateDTO {

    @Size(min = 2, max = 20, message = "昵称长度应在2-20个字符之间")
    private String nickname;

    private String avatarUrl;

    private Integer gender;

    private String phone;

    private LocalDate birthday;
}
