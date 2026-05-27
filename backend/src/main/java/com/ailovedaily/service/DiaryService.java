package com.ailovedaily.service;

import com.ailovedaily.dto.DiaryDTO;
import com.ailovedaily.vo.DiaryVO;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import java.util.List;

/**
 * 日记服务接口
 */
public interface DiaryService {

    /**
     * 发布日记
     */
    void publishDiary(Long userId, DiaryDTO diaryDTO);

    /**
     * 更新日记
     */
    void updateDiary(Long id, Long userId, DiaryDTO diaryDTO);

    /**
     * 删除日记
     */
    void deleteDiary(Long id, Long userId);

    /**
     * 分页查询日记
     */
    Page<DiaryVO> getDiaryPage(Long coupleId, Long userId, Integer page, Integer size);

    /**
     * 获取时间轴数据
     */
    List<DiaryVO> getTimeline(Long coupleId, Long userId, Integer limit);

    /**
     * 获取日记详情
     */
    DiaryVO getDiaryDetail(Long id, Long userId);

    /**
     * 切换收藏状态
     */
    void toggleFavorite(Long id);

    /**
     * 获取收藏的日记
     */
    List<DiaryVO> getFavoriteDiaries(Long coupleId, Long userId);
}
