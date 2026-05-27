package com.ailovedaily.service;

import com.ailovedaily.dto.MenuItemDTO;
import com.ailovedaily.entity.MenuItem;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import java.util.List;

/**
 * 菜单服务接口
 */
public interface MenuService {

    /**
     * 添加菜品
     */
    void addMenuItem(Long userId, MenuItemDTO menuItemDTO);

    /**
     * 更新菜品
     */
    void updateMenuItem(Long id, MenuItemDTO menuItemDTO);

    /**
     * 删除菜品
     */
    void deleteMenuItem(Long id);

    /**
     * 分页查询菜品
     */
    Page<MenuItem> getMenuPage(Long coupleId, Integer category, Integer page, Integer size);

    /**
     * 根据分类查询
     */
    List<MenuItem> getByCategory(Long coupleId, Integer category);

    /**
     * 随机获取一个菜品
     */
    MenuItem getRandomMenu(Long coupleId);

    /**
     * 获取菜品详情
     */
    MenuItem getById(Long id);
}
