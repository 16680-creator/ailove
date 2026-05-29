package com.ailovedaily.controller;

import com.ailovedaily.dto.OutfitGenerateDTO;
import com.ailovedaily.service.OutfitService;
import com.ailovedaily.vo.OutfitVO;
import com.ailovedaily.vo.ResultVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 穿搭方案控制器
 */
@RestController
@RequestMapping("/api/outfit")
@RequiredArgsConstructor
@Tag(name = "穿搭方案", description = "AI穿搭方案管理接口")
public class OutfitController {

    private final OutfitService outfitService;

    @PostMapping("/auto-match")
    @Operation(summary = "AI自动搭配", description = "根据衣橱衣物自动搭配穿搭方案")
    public ResultVO<OutfitVO> autoMatch(@RequestAttribute("userId") Long userId,
                                         @RequestBody OutfitGenerateDTO dto) {
        OutfitVO vo = outfitService.autoMatch(userId, dto);
        return ResultVO.success(vo);
    }

    @PostMapping("/manual")
    @Operation(summary = "手动搭配", description = "用户选择衣物并生成穿搭效果图")
    public ResultVO<OutfitVO> manual(@RequestAttribute("userId") Long userId,
                                      @RequestBody OutfitGenerateDTO dto) {
        OutfitVO vo = outfitService.manualGenerate(userId, dto);
        return ResultVO.success(vo);
    }

    @GetMapping("/list")
    @Operation(summary = "获取穿搭列表", description = "分页获取穿搭方案列表")
    public ResultVO<List<OutfitVO>> list(@RequestAttribute("userId") Long userId,
                                          @RequestParam(defaultValue = "1") int pageNum,
                                          @RequestParam(defaultValue = "20") int pageSize) {
        List<OutfitVO> list = outfitService.list(userId, pageNum, pageSize);
        return ResultVO.success(list);
    }

    @GetMapping("/{id}")
    @Operation(summary = "获取穿搭详情", description = "根据ID获取穿搭方案详情")
    public ResultVO<OutfitVO> detail(@PathVariable Long id,
                                      @RequestAttribute("userId") Long userId) {
        OutfitVO vo = outfitService.detail(id, userId);
        return ResultVO.success(vo);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除穿搭方案", description = "删除指定穿搭方案")
    public ResultVO<Boolean> delete(@PathVariable Long id,
                                     @RequestAttribute("userId") Long userId) {
        Boolean result = outfitService.delete(id, userId);
        return ResultVO.success(result);
    }
}
