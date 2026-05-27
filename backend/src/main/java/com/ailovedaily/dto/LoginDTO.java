package com.ailovedaily.dto;

import lombok.Data;

/**
 * 登录请求DTO
 */
@Data
public class LoginDTO {

    /**
     * 微信登录临时凭证
     */
    private String code;

    /**
     * 用户信息（加密）
     */
    private String encryptedData;

    /**
     * 加密算法的初始向量
     */
    private String iv;
}
