package com.ailovedaily.service.impl;

import cn.hutool.core.util.RandomUtil;
import com.ailovedaily.dto.CoupleBindDTO;
import com.ailovedaily.entity.CoupleLink;
import com.ailovedaily.entity.User;
import com.ailovedaily.exception.BusinessException;
import com.ailovedaily.mapper.CoupleLinkMapper;
import com.ailovedaily.mapper.UserMapper;
import com.ailovedaily.service.CoupleService;
import com.ailovedaily.vo.LoveInfoVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;

/**
 * 情侣关系服务实现。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CoupleServiceImpl implements CoupleService {

    private final CoupleLinkMapper coupleLinkMapper;
    private final UserMapper userMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> createCouple(Long userId, CoupleBindDTO bindDTO) {
        User user = userMapper.selectById(userId);
        if (user != null && user.getCoupleId() != null) {
            throw buildExistingCoupleException(user.getCoupleId());
        }

        String inviteCode = generateInviteCode();

        CoupleLink couple = new CoupleLink();
        couple.setUser1Id(userId);
        couple.setLoveStartDate(bindDTO.getLoveStartDate());
        couple.setLoveMotto(bindDTO.getLoveMotto());
        couple.setInviteCode(inviteCode);
        couple.setStatus(0);
        coupleLinkMapper.insert(couple);

        user.setCoupleId(couple.getId());
        user.setRole(1);
        userMapper.updateById(user);

        Map<String, Object> result = new HashMap<>();
        result.put("coupleId", couple.getId());
        result.put("inviteCode", inviteCode);
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void bindCouple(Long userId, String inviteCode) {
        CoupleLink couple = coupleLinkMapper.selectByInviteCode(inviteCode);
        if (couple == null) {
            throw new RuntimeException("邀请码无效");
        }

        if (couple.getStatus() != null && couple.getStatus() != 0) {
            throw new RuntimeException("该邀请码已被使用");
        }

        if (couple.getUser1Id().equals(userId)) {
            throw new RuntimeException("不能绑定自己创建的关系");
        }

        User user = userMapper.selectById(userId);
        if (user.getCoupleId() != null) {
            throw buildExistingCoupleException(user.getCoupleId());
        }

        couple.setUser2Id(userId);
        couple.setStatus(1);
        couple.setBindTime(java.time.LocalDateTime.now());
        coupleLinkMapper.updateById(couple);

        user.setCoupleId(couple.getId());
        user.setRole(2);
        userMapper.updateById(user);
    }

    @Override
    public CoupleLink getCoupleById(Long coupleId) {
        return coupleLinkMapper.selectById(coupleId);
    }

    @Override
    public LoveInfoVO getLoveInfo(Long coupleId) {
        CoupleLink couple = coupleLinkMapper.selectById(coupleId);
        if (couple == null || couple.getStatus() == null || couple.getStatus() != 1) {
            return null;
        }

        LoveInfoVO loveInfo = new LoveInfoVO();
        loveInfo.setLoveStartDate(couple.getLoveStartDate());
        loveInfo.setLoveMotto(couple.getLoveMotto());

        long days = ChronoUnit.DAYS.between(couple.getLoveStartDate(), LocalDate.now());
        loveInfo.setDaysTogether(days);

        long years = days / 365;
        LocalDate nextAnniversary = couple.getLoveStartDate().plusYears(years + 1);
        long nextDays = ChronoUnit.DAYS.between(LocalDate.now(), nextAnniversary);
        loveInfo.setNextAnniversaryDays(nextDays);

        return loveInfo;
    }

    @Override
    public void updateMotto(Long coupleId, String motto) {
        CoupleLink couple = new CoupleLink();
        couple.setId(coupleId);
        couple.setLoveMotto(motto);
        coupleLinkMapper.updateById(couple);
    }

    @Override
    public void updateCouplePhoto(Long coupleId, String photoUrl) {
        CoupleLink couple = new CoupleLink();
        couple.setId(coupleId);
        couple.setCouplePhoto(photoUrl);
        coupleLinkMapper.updateById(couple);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void unbindCouple(Long coupleId, Long userId) {
        CoupleLink couple = coupleLinkMapper.selectById(coupleId);
        if (couple == null) {
            throw new RuntimeException("情侣关系不存在");
        }

        couple.setStatus(2);
        coupleLinkMapper.updateById(couple);

        User user1 = userMapper.selectById(couple.getUser1Id());
        User user2 = userMapper.selectById(couple.getUser2Id());

        if (user1 != null) {
            user1.setCoupleId(null);
            user1.setRole(null);
            userMapper.updateById(user1);
        }

        if (user2 != null) {
            user2.setCoupleId(null);
            user2.setRole(null);
            userMapper.updateById(user2);
        }
    }

    @Override
    public String generateInviteCode() {
        String code;
        do {
            code = RandomUtil.randomString(6).toUpperCase();
        } while (coupleLinkMapper.selectByInviteCode(code) != null);
        return code;
    }

    private BusinessException buildExistingCoupleException(Long coupleId) {
        CoupleLink existedCouple = coupleLinkMapper.selectById(coupleId);
        Map<String, Object> payload = new HashMap<>();
        payload.put("coupleId", coupleId);

        if (existedCouple != null) {
            payload.put("inviteCode", existedCouple.getInviteCode());
            payload.put("status", existedCouple.getStatus());
            payload.put("loveStartDate", existedCouple.getLoveStartDate());
        }

        return new BusinessException(409, "您已有情侣关系", payload);
    }
}
