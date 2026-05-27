package com.ailovedaily.controller;

import com.ailovedaily.service.HomeService;
import com.ailovedaily.vo.AiQuoteVO;
import com.ailovedaily.vo.HomeVO;
import com.ailovedaily.vo.ResultVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 首页控制器
 */
@RestController
@RequestMapping("/api/home")
@RequiredArgsConstructor
@Tag(name = "首页", description = "首页数据相关接口")
public class HomeController {

    private final HomeService homeService;

    @GetMapping("/data")
    @Operation(summary = "获取首页数据", description = "获取首页所有数据，包括用户信息、恋爱信息、快捷入口等")
    public ResultVO<HomeVO> getHomeData(@RequestAttribute("userId") Long userId) {
        HomeVO homeData = homeService.getHomeData(userId);
        return ResultVO.success(homeData);
    }

    @GetMapping("/quote")
    @Operation(summary = "获取每日一言", description = "获取随机情话")
    public ResultVO<String> getDailyQuote() {
        String quote = homeService.getDailyQuote();
        return ResultVO.success(quote);
    }

    @GetMapping("/ai-quote")
    @Operation(summary = "获取AI每日情话", description = "获取AI生成的个性化情话，优先返回缓存")
    public ResultVO<AiQuoteVO> getAiDailyQuote(@RequestAttribute("userId") Long userId) {
        AiQuoteVO quote = homeService.getAiDailyQuote(userId, false);
        return ResultVO.success(quote);
    }

    @PostMapping("/ai-quote/refresh")
    @Operation(summary = "刷新AI每日情话", description = "强制重新生成AI情话")
    public ResultVO<AiQuoteVO> refreshAiDailyQuote(@RequestAttribute("userId") Long userId) {
        AiQuoteVO quote = homeService.getAiDailyQuote(userId, true);
        return ResultVO.success(quote);
    }
}
