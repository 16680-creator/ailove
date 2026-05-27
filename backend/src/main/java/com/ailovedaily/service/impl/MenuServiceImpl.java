package com.ailovedaily.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.ailovedaily.dto.MenuItemDTO;
import com.ailovedaily.entity.MenuItem;
import com.ailovedaily.entity.User;
import com.ailovedaily.mapper.MenuItemMapper;
import com.ailovedaily.mapper.UserMapper;
import com.ailovedaily.service.DishRecipeService;
import com.ailovedaily.service.MenuService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 菜单服务实现类
 */
@Service
@RequiredArgsConstructor
public class MenuServiceImpl implements MenuService {

    private final MenuItemMapper menuItemMapper;
    private final UserMapper userMapper;
    private final DishRecipeService dishRecipeService;

    @Override
    public void addMenuItem(Long userId, MenuItemDTO menuItemDTO) {
        User user = userMapper.selectById(userId);
        if (user == null || user.getCoupleId() == null) {
            throw new RuntimeException("用户未绑定情侣关系");
        }

        MenuItem menuItem = new MenuItem();
        BeanUtil.copyProperties(menuItemDTO, menuItem);
        menuItem.setCoupleId(user.getCoupleId());
        menuItem.setCreateBy(userId);

        menuItemMapper.insert(menuItem);

        // 异步生成 AI 做法
        dishRecipeService.generateRecipeAsync(menuItem.getId());
    }

    @Override
    public void updateMenuItem(Long id, MenuItemDTO menuItemDTO) {
        MenuItem menuItem = new MenuItem();
        menuItem.setId(id);
        BeanUtil.copyProperties(menuItemDTO, menuItem);
        menuItemMapper.updateById(menuItem);
    }

    @Override
    public void deleteMenuItem(Long id) {
        menuItemMapper.deleteById(id);
    }

    @Override
    public Page<MenuItem> getMenuPage(Long coupleId, Integer category, Integer page, Integer size) {
        LambdaQueryWrapper<MenuItem> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MenuItem::getCoupleId, coupleId);
        if (category != null) {
            wrapper.eq(MenuItem::getCategory, category);
        }
        wrapper.orderByDesc(MenuItem::getCreateTime);

        return menuItemMapper.selectPage(new Page<>(page, size), wrapper);
    }

    @Override
    public List<MenuItem> getByCategory(Long coupleId, Integer category) {
        return menuItemMapper.selectByCoupleIdAndCategory(coupleId, category);
    }

    @Override
    public MenuItem getRandomMenu(Long coupleId) {
        return menuItemMapper.selectRandomByCoupleId(coupleId);
    }

    @Override
    public MenuItem getById(Long id) {
        return menuItemMapper.selectById(id);
    }
}
