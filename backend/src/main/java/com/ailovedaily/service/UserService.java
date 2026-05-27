package com.ailovedaily.service;

import com.ailovedaily.dto.AccountLoginDTO;
import com.ailovedaily.dto.LoginDTO;
import com.ailovedaily.dto.RegisterDTO;
import com.ailovedaily.dto.UserUpdateDTO;
import com.ailovedaily.entity.User;
import com.ailovedaily.vo.UserVO;

import java.util.Map;

/**
 * 用户服务接口
 */
public interface UserService {

    /**
     * 微信登录
     */
    Map<String, Object> wxLogin(LoginDTO loginDTO);

    /**
     * Web 账号密码登录。
     */
    Map<String, Object> accountLogin(AccountLoginDTO loginDTO);

    /**
     * Web 账号注册。
     */
    Map<String, Object> register(RegisterDTO registerDTO);

    /**
     * 开发测试登录（跳过微信鉴权，仅用于本地调试）
     */
    Map<String, Object> devLogin(Long userId);

    /**
     * 根据ID查询用户
     */
    User getById(Long id);

    /**
     * 获取用户信息（包含伴侣信息）
     */
    UserVO getUserInfo(Long userId);

    /**
     * 更新用户信息
     */
    void updateUser(Long userId, UserUpdateDTO updateDTO);

    /**
     * 获取伴侣信息
     */
    UserVO getPartnerInfo(Long userId);
}
