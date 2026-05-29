package com.ailovedaily.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.ailovedaily.entity.CoupleLink;
import com.ailovedaily.entity.DailyQuote;
import com.ailovedaily.entity.User;
import com.ailovedaily.mapper.CoupleLinkMapper;
import com.ailovedaily.mapper.DailyQuoteMapper;
import com.ailovedaily.mapper.UserMapper;
import com.ailovedaily.service.AiQuoteService;
import com.ailovedaily.vo.AiQuoteVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import com.ailovedaily.config.RedisHelper;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * AI 情话服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiQuoteServiceImpl implements AiQuoteService {

    private final CoupleLinkMapper coupleLinkMapper;
    private final UserMapper userMapper;
    private final DailyQuoteMapper dailyQuoteMapper;
    private final RedisHelper redisHelper;

    @Value("${ai.enabled:false}")
    private Boolean aiEnabled;

    @Value("${ai.api-url:}")
    private String aiApiUrl;

    @Value("${ai.api-key:}")
    private String aiApiKey;

    @Value("${ai.model:deepseek-chat}")
    private String aiModel;

    @Value("${ai.timeout:30000}")
    private Integer aiTimeout;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final String AI_QUOTE_CACHE_PREFIX = "ai:quote:";

    @Override
    public AiQuoteVO generateQuote(Long coupleId, boolean force) {
        if (!Boolean.TRUE.equals(aiEnabled) || StrUtil.isBlank(aiApiKey)) {
            log.debug("AI 功能未启用或未配置 API Key，降级为随机情话");
            return fallbackQuote();
        }

        String today = LocalDate.now().format(DATE_FORMATTER);
        String cacheKey = AI_QUOTE_CACHE_PREFIX + coupleId + ":" + today;

        if (!force) {
            String cached = redisHelper.getString(cacheKey);
            if (StrUtil.isNotBlank(cached)) {
                return buildAiQuoteVO(cached, true);
            }

            DailyQuote dbQuote = dailyQuoteMapper.selectByCoupleAndDate(coupleId, today);
            if (dbQuote != null && StrUtil.isNotBlank(dbQuote.getContent())) {
                redisHelper.set(cacheKey, dbQuote.getContent(), 1, TimeUnit.DAYS);
                return buildAiQuoteVO(dbQuote.getContent(), true);
            }
        }

        String quote = callAiApi(coupleId);
        if (StrUtil.isBlank(quote)) {
            log.warn("AI 生成失败，降级为随机情话");
            return fallbackQuote();
        }

        try {
            DailyQuote dailyQuote = new DailyQuote();
            dailyQuote.setContent(quote);
            dailyQuote.setAuthor("AI");
            dailyQuote.setCategory(1);
            dailyQuote.setSource(1);
            dailyQuote.setCoupleId(coupleId);
            dailyQuote.setQuoteDate(LocalDate.now());
            dailyQuote.setUseCount(0);
            dailyQuoteMapper.insert(dailyQuote);
        } catch (Exception e) {
            log.warn("保存 AI 情话到数据库失败", e);
        }

        redisHelper.set(cacheKey, quote, 1, TimeUnit.DAYS);

        return buildAiQuoteVO(quote, true);
    }

    @Override
    public void preGenerateForAllCouples() {
        if (!Boolean.TRUE.equals(aiEnabled) || StrUtil.isBlank(aiApiKey)) {
            log.info("AI 功能未启用，跳过预生成任务");
            return;
        }

        List<CoupleLink> couples = coupleLinkMapper.selectList(
                new LambdaQueryWrapper<CoupleLink>().eq(CoupleLink::getStatus, 1));

        log.info("开始为 {} 对情侣预生成今日 AI 情话", couples.size());
        int success = 0;
        int failed = 0;

        for (CoupleLink couple : couples) {
            try {
                String today = LocalDate.now().format(DATE_FORMATTER);
                DailyQuote existing = dailyQuoteMapper.selectByCoupleAndDate(couple.getId(), today);
                if (existing != null) {
                    continue;
                }

                generateQuote(couple.getId(), true);
                success++;
                Thread.sleep(200);
            } catch (Exception e) {
                log.error("为情侣 {} 预生成 AI 情话失败", couple.getId(), e);
                failed++;
            }
        }

        log.info("AI 情话预生成完成：成功 {}，失败 {}", success, failed);
    }

    private String callAiApi(Long coupleId) {
        CoupleLink couple = coupleLinkMapper.selectById(coupleId);
        if (couple == null) {
            return null;
        }

        User user1 = userMapper.selectById(couple.getUser1Id());
        User user2 = userMapper.selectById(couple.getUser2Id());

        String boyName = user1 != null && user1.getGender() != null && user1.getGender() == 1
                ? user1.getNickname() : (user2 != null ? user2.getNickname() : "TA");
        String girlName = user1 != null && user1.getGender() != null && user1.getGender() == 2
                ? user1.getNickname() : (user2 != null ? user2.getNickname() : "TA");

        if (StrUtil.isBlank(boyName)) boyName = "TA";
        if (StrUtil.isBlank(girlName)) girlName = "TA";

        long daysTogether = ChronoUnit.DAYS.between(couple.getLoveStartDate(), LocalDate.now());
        String today = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy年M月d日"));
        String season = getSeason();

        String prompt = buildPrompt(boyName, girlName, daysTogether, today, season);

        try {
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", aiModel);

            JSONArray messages = new JSONArray();
            JSONObject systemMsg = new JSONObject();
            systemMsg.set("role", "system");
            systemMsg.set("content",
                    "你是一位浪漫的情话大师。"
                    + "你的唯一任务是输出一句中文情话（20-40字）。"
                    + "严格禁止：解释、复述任务、分析背景信息、使用英文、输出标题或标记。"
                    + "只输出情话本身，没有其他任何内容。");
            messages.add(systemMsg);

            JSONObject userMsg = new JSONObject();
            userMsg.set("role", "user");
            userMsg.set("content", prompt);
            messages.add(userMsg);
            requestBody.put("messages", messages);

            requestBody.put("temperature", 0.9);
            requestBody.put("max_tokens", 256);

            String jsonBody = JSONUtil.toJsonStr(requestBody);
            log.debug("AI API 请求: {}", jsonBody);

            HttpResponse response = HttpUtil.createPost(aiApiUrl)
                    .header("Authorization", "Bearer " + aiApiKey)
                    .header("Content-Type", "application/json")
                    .timeout(aiTimeout)
                    .body(jsonBody)
                    .execute();

            if (!response.isOk()) {
                log.error("AI API 调用失败: status={}, body={}", response.getStatus(), response.body());
                return null;
            }

            JSONObject result = JSONUtil.parseObj(response.body());
            log.debug("AI API 原始响应: {}", response.body());

            // MiniMax M2.7 兼容 OpenAI 格式，但可能有 base_resp 错误
            JSONObject baseResp = result.getJSONObject("base_resp");
            if (baseResp != null && baseResp.getInt("status_code", 0) != 0) {
                log.error("MiniMax API 返回错误: {}", baseResp.getStr("status_msg", "unknown"));
                return null;
            }

            JSONArray choices = result.getJSONArray("choices");
            if (choices == null || choices.isEmpty()) {
                log.error("AI API 返回为空: {}", response.body());
                return null;
            }

            JSONObject respMessage = choices.getJSONObject(0).getJSONObject("message");
            String content = respMessage.getStr("content", "");

            // M2.7 思维链模型：content 可能为空，实际内容在 reasoning_content 中
            if (StrUtil.isBlank(content)) {
                content = respMessage.getStr("reasoning_content", "");
            }

            content = cleanQuote(content);
            log.info("AI 情话生成成功: coupleId={}, content={}", coupleId, content);
            return content;

        } catch (Exception e) {
            log.error("AI API 调用异常", e);
            return null;
        }
    }

    private String buildPrompt(String boyName, String girlName, long daysTogether, String today, String season) {
        return "你是一位浪漫的情话大师。请为一对情侣写一句简短的中文情话（不超过40字）。\n\n"
                + "背景信息：\n"
                + "- 男生昵称：" + boyName + "\n"
                + "- 女生昵称：" + girlName + "\n"
                + "- 他们已经在一起 " + daysTogether + " 天\n"
                + "- 今天是 " + today + "，" + season + "\n\n"
                + "要求：\n"
                + "1. 内容温馨浪漫，适合情侣之间表达爱意\n"
                + "2. 可以自然融入他们的昵称和恋爱天数，但不要生硬堆砌\n"
                + "3. 只返回情话内容本身，不要加引号，不要有任何解释、标题或标记\n"
                + "4. 字数控制20-40字之间\n"
                + "5. 风格可以变化：有时甜蜜、有时俷皮、有时深情\n"
                + "6. 不要出现任何英文";
    }

    private String cleanQuote(String content) {
        if (StrUtil.isBlank(content)) {
            return "";
        }
        // 去除思维链标签残留
        content = content.replaceAll("<think[^>]*>[\\s\\S]*?</think\\s*>", "");
        content = content.replaceAll("</?think[^>]*>", "");
        // 去除 markdown 代码块
        content = content.replaceAll("```[a-zA-Z]*[\\s\\S]*?```", "");
        // 去除引号
        content = content.replaceAll("[\"']", "");
        // 去除换行，压缩为空格
        content = content.replace("\r", "").replaceAll("\n+", " ").replaceAll("\\s+", " ");
        // 去除常见前缀
        content = content.replaceAll("^(情话[:：]?|回复[:：]?|答案[:：]?|内容[:：]?|AI[:：]?|输出[:：]?)\\s*", "");
        // 检测是否返回了 prompt 模板文本
        if (isPromptLeak(content)) {
            log.warn("AI 返回了 prompt 文本，降级处理");
            return "";
        }
        content = content.trim();
        if (content.length() > 80) {
            content = content.substring(0, 80);
        }
        return content;
    }

    private boolean isPromptLeak(String content) {
        int markers = 0;
        if (content.contains("背景信息")) markers++;
        if (content.contains("要求")) markers++;
        if (content.contains("男生昵称") || content.contains("女生昵称")) markers++;
        if (content.contains("情话大师")) markers++;
        if (content.contains("为一对情侣")) markers++;
        if (content.contains("他们已经在一起")) markers++;
        if (content.contains("温馨浪漫")) markers++;
        return markers >= 2;
    }

    private String getSeason() {
        int month = LocalDate.now().getMonthValue();
        switch (month) {
            case 3: case 4: case 5:
                return "春意盎然";
            case 6: case 7: case 8:
                return "夏日炎炎";
            case 9: case 10: case 11:
                return "秋高气爽";
            default:
                return "冬日温暖";
        }
    }

    private AiQuoteVO fallbackQuote() {
        DailyQuote quote = dailyQuoteMapper.selectRandomByCategory(1);
        String content = quote != null ? quote.getContent() : "遇见你，是我还辈子最美的意外。";
        return buildAiQuoteVO(content, false);
    }

    private AiQuoteVO buildAiQuoteVO(String content, boolean aiGenerated) {
        AiQuoteVO vo = new AiQuoteVO();
        vo.setContent(content);
        vo.setAiGenerated(aiGenerated);
        vo.setGenerateTime(LocalDate.now().format(DATE_FORMATTER));
        return vo;
    }
}
