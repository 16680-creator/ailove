package com.ailovedaily.controller;

import com.ailovedaily.dto.WardrobeItemUpdateDTO;
import com.ailovedaily.service.WardrobeService;
import com.ailovedaily.vo.ResultVO;
import com.ailovedaily.vo.WardrobeItemVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 衣物管理控制器
 */
@RestController
@RequestMapping("/api/wardrobe")
@RequiredArgsConstructor
@Tag(name = "智能衣柜", description = "衣物管理相关接口")
public class WardrobeController {

    private final WardrobeService wardrobeService;

    @PostMapping("/upload")
    @Operation(summary = "上传衣物图片", description = "上传衣物图片并自动识别分类")
    public ResultVO<WardrobeItemVO> upload(@RequestParam("file") MultipartFile file,
                                           @RequestAttribute("userId") Long userId) {
        WardrobeItemVO vo = wardrobeService.upload(file, userId);
        return ResultVO.success(vo);
    }

    @GetMapping("/list")
    @Operation(summary = "获取衣物列表", description = "按分类和季节筛选衣物")
    public ResultVO<List<WardrobeItemVO>> list(@RequestAttribute("userId") Long userId,
                                                @RequestParam(required = false) String category,
                                                @RequestParam(required = false) String season,
                                                @RequestParam(defaultValue = "false") Boolean partnerView) {
        List<WardrobeItemVO> list = wardrobeService.list(userId, category, season, partnerView);
        return ResultVO.success(list);
    }

    @GetMapping("/{id}")
    @Operation(summary = "获取衣物详情", description = "根据ID获取衣物详情")
    public ResultVO<WardrobeItemVO> detail(@PathVariable Long id,
                                            @RequestAttribute("userId") Long userId) {
        WardrobeItemVO vo = wardrobeService.detail(id, userId);
        return ResultVO.success(vo);
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新衣物信息", description = "更新衣物的分类、颜色、风格等属性")
    public ResultVO<WardrobeItemVO> update(@PathVariable Long id,
                                            @RequestAttribute("userId") Long userId,
                                            @RequestBody WardrobeItemUpdateDTO dto) {
        WardrobeItemVO vo = wardrobeService.update(id, userId, dto);
        return ResultVO.success(vo);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除衣物", description = "删除指定衣物")
    public ResultVO<Boolean> delete(@PathVariable Long id,
                                     @RequestAttribute("userId") Long userId) {
        Boolean result = wardrobeService.delete(id, userId);
        return ResultVO.success(result);
    }

    @PostMapping("/{id}/favorite")
    @Operation(summary = "收藏/取消收藏", description = "切换衣物收藏状态")
    public ResultVO<Boolean> favorite(@PathVariable Long id,
                                       @RequestAttribute("userId") Long userId) {
        Boolean result = wardrobeService.favorite(id, userId);
        return ResultVO.success(result);
    }

    @PostMapping("/{id}/recognize")
    @Operation(summary = "重新识别衣物", description = "使用AI重新识别衣物属性")
    public ResultVO<WardrobeItemVO> recognize(@PathVariable Long id,
                                               @RequestAttribute("userId") Long userId) {
        WardrobeItemVO vo = wardrobeService.recognize(id, userId);
        return ResultVO.success(vo);
    }
}
