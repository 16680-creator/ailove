package com.ailovedaily.controller;

import com.ailovedaily.dto.WishListDTO;
import com.ailovedaily.service.WishService;
import com.ailovedaily.vo.ResultVO;
import com.ailovedaily.vo.WishListVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 心愿控制器
 */
@RestController
@RequestMapping("/api/wish")
@RequiredArgsConstructor
@Tag(name = "心愿清单", description = "心愿管理相关接口")
public class WishController {

    private final WishService wishService;

    @PostMapping
    @Operation(summary = "添加心愿", description = "添加新的心愿")
    public ResultVO<Void> addWish(@RequestAttribute("userId") Long userId,
                                  @RequestBody WishListDTO wishDTO) {
        wishService.addWish(userId, wishDTO);
        return ResultVO.success();
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新心愿", description = "更新心愿信息")
    public ResultVO<Void> updateWish(@PathVariable Long id,
                                     @RequestAttribute("userId") Long userId,
                                     @RequestBody WishListDTO wishDTO) {
        wishService.updateWish(id, userId, wishDTO);
        return ResultVO.success();
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除心愿", description = "删除指定心愿")
    public ResultVO<Void> deleteWish(@PathVariable Long id,
                                     @RequestAttribute("userId") Long userId) {
        wishService.deleteWish(id, userId);
        return ResultVO.success();
    }

    @PostMapping("/{id}/complete")
    @Operation(summary = "完成心愿", description = "标记心愿为已完成，可关联日记和照片")
    public ResultVO<Void> completeWish(@PathVariable Long id,
                                       @RequestAttribute("userId") Long userId,
                                       @RequestParam(required = false) Long diaryId,
                                       @RequestParam(required = false) List<Long> photoIds) {
        wishService.completeWish(id, userId, diaryId, photoIds);
        return ResultVO.success();
    }

    @PostMapping("/{id}/uncomplete")
    @Operation(summary = "取消完成心愿", description = "将已完成的心愿恢复为待完成状态")
    public ResultVO<Void> uncompleteWish(@PathVariable Long id,
                                         @RequestAttribute("userId") Long userId) {
        wishService.uncompleteWish(id, userId);
        return ResultVO.success();
    }

    @GetMapping
    @Operation(summary = "根据状态查询心愿", description = "获取指定状态的心愿列表")
    public ResultVO<List<WishListVO>> getWishesByStatus(@RequestAttribute("coupleId") Long coupleId,
                                                        @RequestParam(required = false) Integer status) {
        List<WishListVO> wishes = wishService.getWishesByStatus(coupleId, status);
        return ResultVO.success(wishes);
    }

    @GetMapping("/category/{category}")
    @Operation(summary = "按分类查询", description = "获取指定分类的心愿")
    public ResultVO<List<WishListVO>> getWishesByCategory(@RequestAttribute("coupleId") Long coupleId,
                                                          @PathVariable Integer category) {
        List<WishListVO> wishes = wishService.getWishesByCategory(coupleId, category);
        return ResultVO.success(wishes);
    }

    @GetMapping("/{id}")
    @Operation(summary = "获取心愿详情", description = "获取指定心愿的详细信息")
    public ResultVO<WishListVO> getWishDetail(@PathVariable Long id) {
        WishListVO wish = wishService.getWishDetail(id);
        return ResultVO.success(wish);
    }

    @GetMapping("/stats")
    @Operation(summary = "获取统计信息", description = "获取心愿各状态的数量统计")
    public ResultVO<Map<String, Object>> getStatusCount(@RequestAttribute("coupleId") Long coupleId) {
        Map<String, Object> stats = wishService.getStatusCount(coupleId);
        return ResultVO.success(stats);
    }
}
