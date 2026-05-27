package com.ailovedaily.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.json.JSONUtil;
import com.ailovedaily.dto.DiaryDTO;
import com.ailovedaily.entity.Diary;
import com.ailovedaily.entity.User;
import com.ailovedaily.exception.NotFoundException;
import com.ailovedaily.exception.UnauthorizedException;
import com.ailovedaily.mapper.DiaryMapper;
import com.ailovedaily.mapper.UserMapper;
import com.ailovedaily.service.DiaryService;
import com.ailovedaily.vo.DiaryVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 日记服务实现类
 */
@Service
@RequiredArgsConstructor
public class DiaryServiceImpl implements DiaryService {

    private final DiaryMapper diaryMapper;
    private final UserMapper userMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void publishDiary(Long userId, DiaryDTO diaryDTO) {
        User user = userMapper.selectById(userId);
        if (user == null || user.getCoupleId() == null) {
            throw new IllegalStateException("用户未绑定情侣关系");
        }

        Diary diary = new Diary();
        BeanUtil.copyProperties(diaryDTO, diary);
        diary.setUserId(userId);
        diary.setCoupleId(user.getCoupleId());

        // 图片数组转JSON
        if (diaryDTO.getImages() != null && !diaryDTO.getImages().isEmpty()) {
            diary.setImages(JSONUtil.toJsonStr(diaryDTO.getImages()));
        }

        diaryMapper.insert(diary);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateDiary(Long id, Long userId, DiaryDTO diaryDTO) {
        Diary diary = diaryMapper.selectById(id);
        if (diary == null) {
            throw NotFoundException.of("日记", id);
        }

        if (!diary.getUserId().equals(userId)) {
            throw new UnauthorizedException("无权修改他人日记");
        }

        BeanUtil.copyProperties(diaryDTO, diary);
        diary.setId(id);

        // 图片数组转JSON
        if (diaryDTO.getImages() != null) {
            diary.setImages(JSONUtil.toJsonStr(diaryDTO.getImages()));
        }

        diaryMapper.updateById(diary);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteDiary(Long id, Long userId) {
        Diary diary = diaryMapper.selectById(id);
        if (diary == null) {
            throw NotFoundException.of("日记", id);
        }

        if (!diary.getUserId().equals(userId)) {
            throw new UnauthorizedException("无权删除他人日记");
        }

        diaryMapper.deleteById(id);
    }

    @Override
    public Page<DiaryVO> getDiaryPage(Long coupleId, Long userId, Integer page, Integer size) {
        LambdaQueryWrapper<Diary> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Diary::getCoupleId, coupleId);
        wrapper.orderByDesc(Diary::getDiaryDate);

        Page<Diary> diaryPage = diaryMapper.selectPage(new Page<>(page, size), wrapper);

        // 批量获取用户信息，避免N+1查询
        List<Diary> diaries = diaryPage.getRecords();
        Map<Long, User> userMap = batchGetUsers(diaries);

        List<DiaryVO> voList = diaries.stream()
                .map(d -> convertToVO(d, userId, userMap))
                .collect(Collectors.toList());

        Page<DiaryVO> resultPage = new Page<>();
        BeanUtil.copyProperties(diaryPage, resultPage);
        resultPage.setRecords(voList);

        return resultPage;
    }

    @Override
    public List<DiaryVO> getTimeline(Long coupleId, Long userId, Integer limit) {
        List<Diary> diaries = diaryMapper.selectTimelineByCoupleId(coupleId, limit);

        // 批量获取用户信息
        Map<Long, User> userMap = batchGetUsers(diaries);

        return diaries.stream()
                .map(d -> convertToVO(d, userId, userMap))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public DiaryVO getDiaryDetail(Long id, Long userId) {
        Diary diary = diaryMapper.selectById(id);
        if (diary == null) {
            throw NotFoundException.of("日记", id);
        }

        // 增加浏览次数
        diaryMapper.incrementViewCount(id);
        diary.setViewCount(diary.getViewCount() + 1);

        // 获取作者信息
        User author = userMapper.selectById(diary.getUserId());
        Map<Long, User> userMap = new HashMap<>();
        if (author != null) {
            userMap.put(author.getId(), author);
        }

        return convertToVO(diary, userId, userMap);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void toggleFavorite(Long id) {
        diaryMapper.toggleFavorite(id);
    }

    @Override
    public List<DiaryVO> getFavoriteDiaries(Long coupleId, Long userId) {
        LambdaQueryWrapper<Diary> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Diary::getCoupleId, coupleId);
        wrapper.eq(Diary::getIsFavorite, 1);
        wrapper.orderByDesc(Diary::getCreateTime);

        List<Diary> diaries = diaryMapper.selectList(wrapper);

        // 批量获取用户信息
        Map<Long, User> userMap = batchGetUsers(diaries);

        return diaries.stream()
                .map(d -> convertToVO(d, userId, userMap))
                .collect(Collectors.toList());
    }

    /**
     * 批量获取用户信息
     */
    private Map<Long, User> batchGetUsers(List<Diary> diaries) {
        Set<Long> userIds = diaries.stream()
                .map(Diary::getUserId)
                .collect(Collectors.toSet());

        if (userIds.isEmpty()) {
            return Collections.emptyMap();
        }

        List<User> users = userMapper.selectBatchIds(userIds);
        return users.stream()
                .collect(Collectors.toMap(User::getId, u -> u));
    }

    /**
     * 转换为VO（使用预加载的用户Map）
     */
    private DiaryVO convertToVO(Diary diary, Long currentUserId, Map<Long, User> userMap) {
        DiaryVO vo = new DiaryVO();
        BeanUtil.copyProperties(diary, vo);

        vo.setIsMine(diary.getUserId().equals(currentUserId));

        // 设置心情文本
        vo.setMoodText(getMoodText(diary.getMood()));

        // 解析图片JSON
        if (diary.getImages() != null && !diary.getImages().isEmpty()) {
            vo.setImages(JSONUtil.toList(diary.getImages(), String.class));
        }

        // 从Map中获取作者信息
        User author = userMap.get(diary.getUserId());
        if (author != null) {
            vo.setUserNickname(author.getNickname());
            vo.setUserAvatar(author.getAvatarUrl());
        }

        return vo;
    }

    private String getMoodText(Integer mood) {
        if (mood == null) return "";
        switch (mood) {
            case 1: return "开心";
            case 2: return "感动";
            case 3: return "平静";
            case 4: return "难过";
            case 5: return "生气";
            default: return "";
        }
    }
}
