package com.ailovedaily.controller;

import com.ailovedaily.dto.TripPlanDTO;
import com.ailovedaily.entity.TripPlan;
import com.ailovedaily.service.TripPlanService;
import com.ailovedaily.vo.ResultVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/trip-plan")
@RequiredArgsConstructor
@Tag(name = "旅行行程", description = "AI 旅行行程规划相关接口")
public class TripPlanController {

    private final TripPlanService tripPlanService;

    @PostMapping("/generate")
    @Operation(summary = "启动生成旅行行程", description = "异步生成，返回行程 ID")
    public ResultVO<Map<String, Object>> generate(@Valid @RequestBody TripPlanDTO dto,
                                                   @RequestAttribute("userId") Long userId) {
        Long id = tripPlanService.startGenerate(dto, userId);
        Map<String, Object> data = new HashMap<>();
        data.put("id", id);
        data.put("status", 0);
        return ResultVO.success(data);
    }

    @GetMapping("/{id}")
    @Operation(summary = "查询行程详情")
    public ResultVO<TripPlan> getById(@PathVariable Long id,
                                       @RequestAttribute("userId") Long userId) {
        TripPlan plan = tripPlanService.getById(id);
        if (plan == null || !plan.getUserId().equals(userId)) {
            return ResultVO.error(404, "行程不存在");
        }
        return ResultVO.success(plan);
    }

    @GetMapping("/list")
    @Operation(summary = "查询行程列表")
    public ResultVO<List<TripPlan>> list(@RequestAttribute("userId") Long userId) {
        return ResultVO.success(tripPlanService.getUserPlans(userId));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除行程")
    public ResultVO<Void> delete(@PathVariable Long id,
                                  @RequestAttribute("userId") Long userId) {
        tripPlanService.delete(id, userId);
        return ResultVO.success(null);
    }
}
