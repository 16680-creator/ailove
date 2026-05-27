package com.ailovedaily.service;

import com.ailovedaily.dto.CoupleBindDTO;
import com.ailovedaily.entity.CoupleLink;
import com.ailovedaily.vo.LoveInfoVO;

import java.util.Map;

/**
 * 情侣关系服务接口
 */
public interface CoupleService {

    /**
     * 创建情侣关系（生成邀请码）
     */
    Map<String, Object> createCouple(Long userId, CoupleBindDTO bindDTO);

    /**
     * 绑定情侣关系
     */
    void bindCouple(Long userId, String inviteCode);

    /**
     * 获取情侣关系
     */
    CoupleLink getCoupleById(Long coupleId);

    /**
     * 获取恋爱信息
     */
    LoveInfoVO getLoveInfo(Long coupleId);

    /**
     * 更新恋爱宣言
     */
    void updateMotto(Long coupleId, String motto);

    /**
     * 更新合照
     */
    void updateCouplePhoto(Long coupleId, String photoUrl);

    /**
     * 解除绑定
     */
    void unbindCouple(Long coupleId, Long userId);

    /**
     * 生成邀请码
     */
    String generateInviteCode();
}
