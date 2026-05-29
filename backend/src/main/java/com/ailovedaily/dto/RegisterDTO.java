package com.ailovedaily.dto;

import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 注册 DTO。
 */
@Data
public class RegisterDTO {

    @NotBlank(message = "登录账号不能为空")
    @Pattern(regexp = "^[A-Za-z0-9_]{4,32}$", message = "登录账号仅支持字母、数字和下划线，长度 4 到 32")
    private String loginName;

    @NotBlank(message = "昵称不能为空")
    @Size(min = 2, max = 20, message = "昵称长度需在 2 到 20 个字符之间")
    private String nickname;

    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 32, message = "密码长度需在 6 到 32 个字符之间")
    private String password;

    @Pattern(regexp = "^$|^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;
}
