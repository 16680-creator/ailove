package com.ailovedaily.controller;

import com.ailovedaily.dto.PeriodDailyLogDTO;
import com.ailovedaily.dto.PeriodRecordDTO;
import com.ailovedaily.service.PeriodService;
import com.ailovedaily.vo.PeriodDailyLogVO;
import com.ailovedaily.vo.PeriodInfoVO;
import com.ailovedaily.vo.PeriodRecordVO;
import com.ailovedaily.vo.ResultVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;

/**
 * 生理期控制器
 */
@RestController
@RequestMapping("/api/period")
@RequiredArgsConstructor
@Tag(name = "姨妈助手", description = "生理期记录和预测相关接口")
public class PeriodController {

    private final PeriodService periodService;

    @PostMapping
    @Operation(summary = "记录生理期", description = "记录生理期开始和结束时间")
    public ResultVO<Void> recordPeriod(@RequestAttribute("userId") Long userId,
                                       @Valid @RequestBody PeriodRecordDTO recordDTO) {
        periodService.recordPeriod(userId, recordDTO);
        return ResultVO.success();
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新生理期记录", description = "更新已有的生理期记录")
    public ResultVO<Void> updatePeriod(@PathVariable Long id,
                                       @RequestAttribute("userId") Long userId,
                                       @Valid @RequestBody PeriodRecordDTO recordDTO) {
        periodService.updatePeriod(id, userId, recordDTO);
        return ResultVO.success();
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除生理期记录", description = "删除指定的生理期记录")
    public ResultVO<Void> deletePeriod(@PathVariable Long id,
                                       @RequestAttribute("userId") Long userId) {
        periodService.deletePeriod(id, userId);
        return ResultVO.success();
    }

    @GetMapping("/info")
    @Operation(summary = "获取生理期信息", description = "获取生理期信息，包含预测数据")
    public ResultVO<PeriodInfoVO> getPeriodInfo(@RequestAttribute("userId") Long userId) {
        PeriodInfoVO info = periodService.getPeriodInfo(userId);
        return ResultVO.success(info);
    }

    @GetMapping("/records")
    @Operation(summary = "获取最近记录", description = "获取最近的生理期记录")
    public ResultVO<List<PeriodRecordVO>> getRecentRecords(@RequestAttribute("userId") Long userId,
                                                           @RequestParam(defaultValue = "6") Integer limit) {
        List<PeriodRecordVO> records = periodService.getRecentRecords(userId, limit);
        return ResultVO.success(records);
    }

    @PostMapping("/predict")
    @Operation(summary = "生成预测", description = "根据历史记录生成未来生理期预测")
    public ResultVO<Void> generatePredictions(@RequestAttribute("userId") Long userId) {
        periodService.generatePredictions(userId);
        return ResultVO.success();
    }

    @PostMapping("/daily-log")
    @Operation(summary = "每日打卡", description = "保存/更新每日经期打卡记录")
    public ResultVO<Void> saveDailyLog(@RequestAttribute("userId") Long userId,
                                       @Valid @RequestBody PeriodDailyLogDTO dto) {
        periodService.saveDailyLog(userId, dto);
        return ResultVO.success();
    }

    @GetMapping("/daily-log")
    @Operation(summary = "获取月度日志", description = "获取指定月份的每日打卡记录")
    public ResultVO<List<PeriodDailyLogVO>> getMonthlyLogs(@RequestAttribute("userId") Long userId,
                                                           @RequestParam int year,
                                                           @RequestParam int month) {
        return ResultVO.success(periodService.getMonthlyLogs(userId, year, month));
    }

    @GetMapping("/daily-log/{date}")
    @Operation(summary = "获取某天日志", description = "获取指定日期的打卡记录")
    public ResultVO<PeriodDailyLogVO> getDailyLog(@RequestAttribute("userId") Long userId,
                                                  @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResultVO.success(periodService.getDailyLog(userId, date));
    }
}
