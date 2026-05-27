package com.ailovedaily.controller;

import com.ailovedaily.dto.MenuItemDTO;
import com.ailovedaily.entity.MenuItem;
import com.ailovedaily.service.DishRecipeService;
import com.ailovedaily.service.MenuService;
import com.ailovedaily.vo.ResultVO;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 菜单控制器
 */
@RestController
@RequestMapping("/api/menu")
@RequiredArgsConstructor
@Tag(name = "共享菜单", description = "菜品管理相关接口")
public class MenuController {

    private final MenuService menuService;
    private final DishRecipeService dishRecipeService;

    @PostMapping
    @Operation(summary = "添加菜品", description = "添加新菜品到菜单")
    public ResultVO<Void> addMenuItem(@RequestAttribute("userId") Long userId,
                                      @RequestBody MenuItemDTO menuItemDTO) {
        menuService.addMenuItem(userId, menuItemDTO);
        return ResultVO.success();
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新菜品", description = "更新菜品信息")
    public ResultVO<Void> updateMenuItem(@PathVariable Long id,
                                         @RequestBody MenuItemDTO menuItemDTO) {
        menuService.updateMenuItem(id, menuItemDTO);
        return ResultVO.success();
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除菜品", description = "删除指定菜品")
    public ResultVO<Void> deleteMenuItem(@PathVariable Long id) {
        menuService.deleteMenuItem(id);
        return ResultVO.success();
    }

    @GetMapping
    @Operation(summary = "分页查询菜品", description = "分页获取菜品列表，支持按分类筛选")
    public ResultVO<Page<MenuItem>> getMenuPage(@RequestAttribute("coupleId") Long coupleId,
                                                @RequestParam(required = false) Integer category,
                                                @RequestParam(defaultValue = "1") Integer page,
                                                @RequestParam(defaultValue = "10") Integer size) {
        Page<MenuItem> menuPage = menuService.getMenuPage(coupleId, category, page, size);
        return ResultVO.success(menuPage);
    }

    @GetMapping("/category/{category}")
    @Operation(summary = "按分类查询", description = "获取指定分类的所有菜品")
    public ResultVO<List<MenuItem>> getByCategory(@RequestAttribute("coupleId") Long coupleId,
                                                  @PathVariable Integer category) {
        List<MenuItem> menuItems = menuService.getByCategory(coupleId, category);
        return ResultVO.success(menuItems);
    }

    @GetMapping("/random")
    @Operation(summary = "随机抽取", description = "随机抽取一个菜品，解决选择困难症")
    public ResultVO<MenuItem> getRandomMenu(@RequestAttribute("coupleId") Long coupleId) {
        MenuItem menuItem = menuService.getRandomMenu(coupleId);
        return ResultVO.success(menuItem);
    }

    @GetMapping("/{id}")
    @Operation(summary = "获取菜品详情", description = "获取指定菜品的详细信息")
    public ResultVO<MenuItem> getMenuDetail(@PathVariable Long id) {
        MenuItem menuItem = menuService.getById(id);
        return ResultVO.success(menuItem);
    }

    @PostMapping("/{id}/recipe")
    @Operation(summary = "生成菜品做法", description = "调用 AI 为指定菜品生成做法")
    public ResultVO<MenuItem> generateRecipe(@PathVariable Long id) {
        dishRecipeService.generateRecipe(id);
        MenuItem menuItem = menuService.getById(id);
        return ResultVO.success(menuItem);
    }
}
