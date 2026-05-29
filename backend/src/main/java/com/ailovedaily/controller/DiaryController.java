package com.ailovedaily.controller;

import com.ailovedaily.dto.DiaryDTO;
import com.ailovedaily.service.DiaryService;
import com.ailovedaily.vo.DiaryVO;
import com.ailovedaily.vo.ResultVO;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

/**
 * 日记控制器
 */
@RestController
@RequestMapping("/api/diary")
@RequiredArgsConstructor
@Tag(name = "情侣日记", description = "日记管理相关接口")
public class DiaryController {

    private final DiaryService diaryService;

    @PostMapping
    @Operation(summary = "发布日记", description = "发布新的日记")
    public ResultVO<Void> publishDiary(@RequestAttribute("userId") Long userId,
                                       @Valid @RequestBody DiaryDTO diaryDTO) {
        diaryService.publishDiary(userId, diaryDTO);
        return ResultVO.success();
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新日记", description = "更新已有日记")
    public ResultVO<Void> updateDiary(@PathVariable Long id,
                                      @RequestAttribute("userId") Long userId,
                                      @Valid @RequestBody DiaryDTO diaryDTO) {
        diaryService.updateDiary(id, userId, diaryDTO);
        return ResultVO.success();
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除日记", description = "删除指定日记")
    public ResultVO<Void> deleteDiary(@PathVariable Long id,
                                      @RequestAttribute("userId") Long userId) {
        diaryService.deleteDiary(id, userId);
        return ResultVO.success();
    }

    @GetMapping
    @Operation(summary = "分页查询日记", description = "分页获取日记列表")
    public ResultVO<Page<DiaryVO>> getDiaryPage(@RequestAttribute("coupleId") Long coupleId,
                                                @RequestAttribute("userId") Long userId,
                                                @RequestParam(defaultValue = "1") Integer page,
                                                @RequestParam(defaultValue = "10") Integer size) {
        Page<DiaryVO> diaryPage = diaryService.getDiaryPage(coupleId, userId, page, size);
        return ResultVO.success(diaryPage);
    }

    @GetMapping("/timeline")
    @Operation(summary = "获取时间轴", description = "获取日记时间轴数据")
    public ResultVO<List<DiaryVO>> getTimeline(@RequestAttribute("coupleId") Long coupleId,
                                               @RequestAttribute("userId") Long userId,
                                               @RequestParam(defaultValue = "20") Integer limit) {
        List<DiaryVO> timeline = diaryService.getTimeline(coupleId, userId, limit);
        return ResultVO.success(timeline);
    }

    @GetMapping("/{id}")
    @Operation(summary = "获取日记详情", description = "获取指定日记的详细信息")
    public ResultVO<DiaryVO> getDiaryDetail(@PathVariable Long id,
                                            @RequestAttribute("userId") Long userId) {
        DiaryVO diary = diaryService.getDiaryDetail(id, userId);
        return ResultVO.success(diary);
    }

    @PostMapping("/{id}/favorite")
    @Operation(summary = "切换收藏状态", description = "收藏或取消收藏日记")
    public ResultVO<Void> toggleFavorite(@PathVariable Long id) {
        diaryService.toggleFavorite(id);
        return ResultVO.success();
    }

    @GetMapping("/favorites")
    @Operation(summary = "获取收藏的日记", description = "获取所有收藏的日记")
    public ResultVO<List<DiaryVO>> getFavoriteDiaries(@RequestAttribute("coupleId") Long coupleId,
                                                      @RequestAttribute("userId") Long userId) {
        List<DiaryVO> favorites = diaryService.getFavoriteDiaries(coupleId, userId);
        return ResultVO.success(favorites);
    }
}
