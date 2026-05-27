package com.ailovedaily.controller;

import com.ailovedaily.dto.MealRecordCreateDTO;
import com.ailovedaily.dto.MealReviewDTO;
import com.ailovedaily.service.MealRecordService;
import com.ailovedaily.vo.MealRecordVO;
import com.ailovedaily.vo.ResultVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 餐食记录控制器
 */
@RestController
@RequestMapping("/api/meal-record")
@RequiredArgsConstructor
@Tag(name = "餐食记录", description = "餐食记录与评价相关接口")
public class MealRecordController {

    private final MealRecordService mealRecordService;

    @PostMapping
    @Operation(summary = "创建餐食记录", description = "下单时保存餐食记录")
    public ResultVO<Void> createMealRecord(@RequestAttribute("userId") Long userId,
                                            @RequestBody MealRecordCreateDTO dto) {
        mealRecordService.createMealRecord(userId, dto);
        return ResultVO.success();
    }

    @PostMapping("/{id}/review")
    @Operation(summary = "添加评价", description = "对餐食记录进行评价，每条记录限一次")
    public ResultVO<Void> addReview(@RequestAttribute("userId") Long userId,
                                     @PathVariable Long id,
                                     @RequestBody MealReviewDTO dto) {
        mealRecordService.addReview(userId, id, dto);
        return ResultVO.success();
    }

    @GetMapping("/history")
    @Operation(summary = "获取历史记录", description = "按天分组获取最近N天的用餐记录")
    public ResultVO<Map<String, List<MealRecordVO>>> getHistory(@RequestAttribute("userId") Long userId,
                                                                 @RequestParam(defaultValue = "7") Integer days) {
        Map<String, List<MealRecordVO>> history = mealRecordService.getHistory(userId, days);
        return ResultVO.success(history);
    }
}
