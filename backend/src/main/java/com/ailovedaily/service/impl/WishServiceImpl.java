package com.ailovedaily.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.json.JSONUtil;
import com.ailovedaily.dto.WishListDTO;
import com.ailovedaily.entity.User;
import com.ailovedaily.entity.WishList;
import com.ailovedaily.mapper.UserMapper;
import com.ailovedaily.mapper.WishListMapper;
import com.ailovedaily.service.WishService;
import com.ailovedaily.vo.WishListVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 心愿服务实现类
 */
@Service
@RequiredArgsConstructor
public class WishServiceImpl implements WishService {

    private final WishListMapper wishListMapper;
    private final UserMapper userMapper;

    @Override
    public void addWish(Long userId, WishListDTO wishDTO) {
        User user = userMapper.selectById(userId);
        if (user == null || user.getCoupleId() == null) {
            throw new RuntimeException("用户未绑定情侣关系");
        }

        WishList wish = new WishList();
        BeanUtil.copyProperties(wishDTO, wish);
        wish.setUserId(userId);
        wish.setCoupleId(user.getCoupleId());
        wish.setStatus(0); // 待完成

        // 关联照片转JSON
        if (wishDTO.getLinkedPhotoIds() != null && !wishDTO.getLinkedPhotoIds().isEmpty()) {
            wish.setLinkedPhotoIds(JSONUtil.toJsonStr(wishDTO.getLinkedPhotoIds()));
        }

        wishListMapper.insert(wish);
    }

    @Override
    public void updateWish(Long id, Long userId, WishListDTO wishDTO) {
        WishList wish = wishListMapper.selectById(id);
        if (wish == null) {
            throw new RuntimeException("心愿不存在");
        }

        BeanUtil.copyProperties(wishDTO, wish);
        wish.setId(id);

        // 关联照片转JSON
        if (wishDTO.getLinkedPhotoIds() != null) {
            wish.setLinkedPhotoIds(JSONUtil.toJsonStr(wishDTO.getLinkedPhotoIds()));
        }

        wishListMapper.updateById(wish);
    }

    @Override
    public void deleteWish(Long id, Long userId) {
        WishList wish = wishListMapper.selectById(id);
        if (wish == null) {
            throw new RuntimeException("心愿不存在");
        }

        wishListMapper.deleteById(id);
    }

    @Override
    public void completeWish(Long id, Long userId, Long diaryId, List<Long> photoIds) {
        WishList wish = wishListMapper.selectById(id);
        if (wish == null) {
            throw new RuntimeException("心愿不存在");
        }

        wish.setStatus(2); // 已完成
        wish.setCompleteDate(LocalDate.now());
        wish.setCompleteBy(userId);
        wish.setLinkedDiaryId(diaryId);

        if (photoIds != null && !photoIds.isEmpty()) {
            wish.setLinkedPhotoIds(JSONUtil.toJsonStr(photoIds));
        }

        wishListMapper.updateById(wish);
    }

    @Override
    public void uncompleteWish(Long id, Long userId) {
        WishList wish = wishListMapper.selectById(id);
        if (wish == null) {
            throw new RuntimeException("心愿不存在");
        }

        wish.setStatus(0); // 待完成
        wish.setCompleteDate(null);
        wish.setCompleteBy(null);
        wish.setLinkedDiaryId(null);
        wish.setLinkedPhotoIds(null);

        wishListMapper.updateById(wish);
    }

    @Override
    public List<WishListVO> getWishesByStatus(Long coupleId, Integer status) {
        List<WishList> wishes = wishListMapper.selectByCoupleIdAndStatus(coupleId, status);
        return wishes.stream().map(this::convertToVO).collect(Collectors.toList());
    }

    @Override
    public List<WishListVO> getWishesByCategory(Long coupleId, Integer category) {
        List<WishList> wishes = wishListMapper.selectByCategory(coupleId, category);
        return wishes.stream().map(this::convertToVO).collect(Collectors.toList());
    }

    @Override
    public WishListVO getWishDetail(Long id) {
        WishList wish = wishListMapper.selectById(id);
        if (wish == null) {
            return null;
        }
        return convertToVO(wish);
    }

    @Override
    public Map<String, Object> getStatusCount(Long coupleId) {
        List<Map<String, Object>> counts = wishListMapper.countByStatus(coupleId);
        Map<String, Object> result = new HashMap<>();

        int total = 0;
        int completed = 0;

        for (Map<String, Object> count : counts) {
            Integer status = (Integer) count.get("status");
            Long cnt = (Long) count.get("count");
            total += cnt;

            if (status == 2) {
                completed = cnt.intValue();
            }
        }

        result.put("total", total);
        result.put("completed", completed);
        result.put("pending", total - completed);

        return result;
    }

    private WishListVO convertToVO(WishList wish) {
        WishListVO vo = new WishListVO();
        BeanUtil.copyProperties(wish, vo);

        // 设置分类文本
        vo.setCategoryText(getCategoryText(wish.getCategory()));

        // 设置优先级文本
        vo.setPriorityText(getPriorityText(wish.getPriority()));

        // 设置状态文本
        vo.setStatusText(getStatusText(wish.getStatus()));

        // 获取创建人信息
        User creator = userMapper.selectById(wish.getUserId());
        if (creator != null) {
            vo.setUserNickname(creator.getNickname());
        }

        // 获取完成人信息
        if (wish.getCompleteBy() != null) {
            User completer = userMapper.selectById(wish.getCompleteBy());
            if (completer != null) {
                vo.setCompleteByNickname(completer.getNickname());
            }
        }

        // 解析关联照片
        if (wish.getLinkedPhotoIds() != null && !wish.getLinkedPhotoIds().isEmpty()) {
            vo.setLinkedPhotoIds(JSONUtil.toList(wish.getLinkedPhotoIds(), Long.class));
        }

        return vo;
    }

    private String getCategoryText(Integer category) {
        if (category == null) return "其他";
        switch (category) {
            case 1: return "旅行";
            case 2: return "美食";
            case 3: return "购物";
            case 4: return "体验";
            default: return "其他";
        }
    }

    private String getPriorityText(Integer priority) {
        if (priority == null) return "中";
        switch (priority) {
            case 1: return "低";
            case 2: return "中";
            case 3: return "高";
            case 4: return "紧急";
            default: return "中";
        }
    }

    private String getStatusText(Integer status) {
        if (status == null) return "待完成";
        switch (status) {
            case 0: return "待完成";
            case 1: return "进行中";
            case 2: return "已完成";
            case 3: return "已放弃";
            default: return "待完成";
        }
    }
}
