package com.ailovedaily.dto;

import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 账号密码登录 DTO。
 */
@Data
public class AccountLoginDTO {

    @NotBlank(message = "登录账号不能为空")
    @Size(min = 4, max = 32, message = "登录账号长度需在 4 到 32 个字符之间")
    private String loginName;

    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 32, message = "密码长度需在 6 到 32 个字符之间")
    private String password;
}
