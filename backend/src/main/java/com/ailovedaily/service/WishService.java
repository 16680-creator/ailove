package com.ailovedaily.service;

import com.ailovedaily.dto.WishListDTO;
import com.ailovedaily.vo.WishListVO;

import java.util.List;
import java.util.Map;

/**
 * 心愿服务接口
 */
public interface WishService {

    /**
     * 添加心愿
     */
    void addWish(Long userId, WishListDTO wishDTO);

    /**
     * 更新心愿
     */
    void updateWish(Long id, Long userId, WishListDTO wishDTO);

    /**
     * 删除心愿
     */
    void deleteWish(Long id, Long userId);

    /**
     * 完成心愿
     */
    void completeWish(Long id, Long userId, Long diaryId, List<Long> photoIds);

    /**
     * 取消完成心愿
     */
    void uncompleteWish(Long id, Long userId);

    /**
     * 根据状态查询心愿
     */
    List<WishListVO> getWishesByStatus(Long coupleId, Integer status);

    /**
     * 根据分类查询心愿
     */
    List<WishListVO> getWishesByCategory(Long coupleId, Integer category);

    /**
     * 获取心愿详情
     */
    WishListVO getWishDetail(Long id);

    /**
     * 统计各状态数量
     */
    Map<String, Object> getStatusCount(Long coupleId);
}
