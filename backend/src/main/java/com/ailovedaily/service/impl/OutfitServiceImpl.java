package com.ailovedaily.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.ailovedaily.dto.OutfitGenerateDTO;
import com.ailovedaily.entity.Outfit;
import com.ailovedaily.entity.OutfitItem;
import com.ailovedaily.entity.User;
import com.ailovedaily.entity.WardrobeItem;
import com.ailovedaily.exception.BusinessException;
import com.ailovedaily.mapper.OutfitItemMapper;
import com.ailovedaily.mapper.OutfitMapper;
import com.ailovedaily.mapper.UserMapper;
import com.ailovedaily.mapper.WardrobeItemMapper;
import com.ailovedaily.service.AiImageService;
import com.ailovedaily.service.OutfitService;
import com.ailovedaily.vo.OutfitVO;
import com.ailovedaily.vo.WardrobeItemVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import com.ailovedaily.config.RedisHelper;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 穿搭方案服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OutfitServiceImpl implements OutfitService {

    private final OutfitMapper outfitMapper;
    private final OutfitItemMapper outfitItemMapper;
    private final WardrobeItemMapper wardrobeItemMapper;
    private final UserMapper userMapper;
    private final AiImageService aiImageService;

    private final RedisHelper redisHelper;

    @Value("${ai.enabled:false}")
    private Boolean aiEnabled;

    @Value("${ai.api-url:}")
    private String aiApiUrl;

    @Value("${ai.api-key:}")
    private String aiApiKey;

    @Value("${ai.model:MiniMax-M2.7}")
    private String aiModel;

    @Value("${ai.timeout:300000}")
    private Integer aiTimeout;

    @Value("${ai.image.daily-limit:20}")
    private Integer dailyLimit;

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");

    @Override
    public OutfitVO autoMatch(Long userId, OutfitGenerateDTO dto) {
        checkAiEnabled();
        checkDailyLimit(userId);

        // 拉取用户全部衣物
        List<WardrobeItem> allItems = wardrobeItemMapper.selectList(
                new LambdaQueryWrapper<WardrobeItem>()
                        .eq(WardrobeItem::getUserId, userId)
                        .eq(WardrobeItem::getDeleted, 0));

        if (allItems.isEmpty()) {
            throw new BusinessException("衣橱为空，请先添加衣物");
        }

        // 构建简化 JSON 供 AI 参考
        JSONArray itemsJson = new JSONArray();
        for (WardrobeItem item : allItems) {
            JSONObject obj = new JSONObject();
            obj.set("id", item.getId());
            obj.set("categoryCode", item.getCategoryCode());
            obj.set("color", item.getColor());
            obj.set("style", item.getStyle());
            obj.set("season", item.getSeason());
            obj.set("occasion", item.getOccasion());
            itemsJson.add(obj);
        }

        String userPrompt = StrUtil.isNotBlank(dto.getPrompt())
                ? "用户需求：" + dto.getPrompt() + "\n\n"
                : "请根据当前季节和衣物自动搭配一套穿搭。\n\n";

        String prompt = userPrompt + "可用衣物列表：\n" + itemsJson.toString()
                + "\n\n请从列表中选择衣物进行搭配，严格只输出 JSON：{\"itemIds\":[衣物ID数组],\"reason\":\"搭配理由\",\"visualPrompt\":\"用于AI生图的英文描述\"}";

        // 调用文本模型获取搭配方案
        JSONObject matchResult = callTextAi(prompt);
        JSONArray itemIdsArr = matchResult.getJSONArray("itemIds");
        String reason = matchResult.getStr("reason", "");
        String visualPrompt = matchResult.getStr("visualPrompt", "");

        List<Long> selectedItemIds = new ArrayList<>();
        if (itemIdsArr != null) {
            for (int i = 0; i < itemIdsArr.size(); i++) {
                selectedItemIds.add(itemIdsArr.getLong(i));
            }
        }

        if (selectedItemIds.isEmpty()) {
            throw new BusinessException("AI 搭配失败：未选择任何衣物");
        }

        // 收集所选衣物的图片 URL
        List<String> referenceImages = new ArrayList<>();
        for (Long itemId : selectedItemIds) {
            WardrobeItem item = wardrobeItemMapper.selectById(itemId);
            if (item != null) {
                referenceImages.add(item.getImageUrl());
            }
        }

        // 生成穿搭效果图
        String outfitImageUrl = aiImageService.generateOutfitImage(visualPrompt, referenceImages);

        // 落库
        return saveOutfit(userId, dto.getPrompt(), reason, outfitImageUrl, selectedItemIds, allItems);
    }

    @Override
    public OutfitVO manualGenerate(Long userId, OutfitGenerateDTO dto) {
        checkAiEnabled();
        checkDailyLimit(userId);

        List<Long> itemIds = dto.getItemIds();
        if (itemIds == null || itemIds.isEmpty()) {
            throw new BusinessException("请选择至少一件衣物");
        }

        // 收集所选衣物信息
        List<WardrobeItem> selectedItems = new ArrayList<>();
        List<String> referenceImages = new ArrayList<>();
        StringBuilder visualPromptBuilder = new StringBuilder("A stylish outfit combination featuring: ");

        for (Long itemId : itemIds) {
            WardrobeItem item = wardrobeItemMapper.selectById(itemId);
            if (item == null) {
                throw new BusinessException("衣物不存在: " + itemId);
            }
            selectedItems.add(item);
            referenceImages.add(item.getImageUrl());

            if (item.getColor() != null) visualPromptBuilder.append(item.getColor()).append(" ");
            if (item.getSubType() != null) visualPromptBuilder.append(item.getSubType()).append(", ");
            if (item.getStyle() != null) visualPromptBuilder.append(item.getStyle()).append(" style, ");
        }

        if (StrUtil.isNotBlank(dto.getPrompt())) {
            visualPromptBuilder.append("User preference: ").append(dto.getPrompt());
        }

        String visualPrompt = visualPromptBuilder.toString();
        String outfitImageUrl = aiImageService.generateOutfitImage(visualPrompt, referenceImages);

        // 获取全部衣物用于 slot 映射
        List<WardrobeItem> allItems = wardrobeItemMapper.selectList(
                new LambdaQueryWrapper<WardrobeItem>()
                        .eq(WardrobeItem::getUserId, userId)
                        .eq(WardrobeItem::getDeleted, 0));

        return saveOutfit(userId, dto.getPrompt(), "", outfitImageUrl, itemIds, allItems);
    }

    @Override
    public List<OutfitVO> list(Long userId, int pageNum, int pageSize) {
        int offset = (pageNum - 1) * pageSize;
        List<Outfit> outfits = outfitMapper.selectByUserOrderByCreateTime(userId, offset, pageSize);
        return outfits.stream().map(this::toVO).collect(Collectors.toList());
    }

    @Override
    public OutfitVO detail(Long id, Long userId) {
        Outfit outfit = outfitMapper.selectById(id);
        if (outfit == null) {
            throw new BusinessException("穿搭方案不存在");
        }
        if (!outfit.getUserId().equals(userId)) {
            throw new BusinessException(403, "无权操作");
        }
        return toVO(outfit);
    }

    @Override
    public Boolean delete(Long id, Long userId) {
        Outfit outfit = outfitMapper.selectById(id);
        if (outfit == null) {
            throw new BusinessException("穿搭方案不存在");
        }
        if (!outfit.getUserId().equals(userId)) {
            throw new BusinessException(403, "无权操作");
        }
        outfitMapper.deleteById(id);
        return true;
    }

    private OutfitVO saveOutfit(Long userId, String prompt, String reason,
                                 String imageUrl, List<Long> itemIds, List<WardrobeItem> allItems) {
        User user = userMapper.selectById(userId);

        Outfit outfit = new Outfit();
        outfit.setUserId(userId);
        outfit.setCoupleId(user != null ? user.getCoupleId() : null);
        outfit.setTitle("AI 搭配方案");
        outfit.setOccasion("daily");
        outfit.setPrompt(prompt);
        outfit.setAiGeneratedImageUrl(imageUrl);
        outfit.setItemIds(JSONUtil.toJsonStr(itemIds));
        outfit.setReason(reason);
        outfitMapper.insert(outfit);

        // 保存关联衣物
        Map<Long, String> categorySlotMap = new HashMap<>();
        categorySlotMap.put(0L, "top"); // 用 categoryCode 映射 slot
        for (Long itemId : itemIds) {
            WardrobeItem matched = allItems.stream()
                    .filter(i -> i.getId().equals(itemId))
                    .findFirst().orElse(null);
            String slot = matched != null ? matched.getCategoryCode() : "other";

            OutfitItem outfitItem = new OutfitItem();
            outfitItem.setOutfitId(outfit.getId());
            outfitItem.setWardrobeItemId(itemId);
            outfitItem.setSlot(slot);
            outfitItemMapper.insert(outfitItem);
        }

        return toVO(outfit);
    }

    private OutfitVO toVO(Outfit outfit) {
        OutfitVO vo = new OutfitVO();
        vo.setId(outfit.getId());
        vo.setUserId(outfit.getUserId());
        vo.setTitle(outfit.getTitle());
        vo.setOccasion(outfit.getOccasion());
        vo.setPrompt(outfit.getPrompt());
        vo.setAiGeneratedImageUrl(outfit.getAiGeneratedImageUrl());
        vo.setReason(outfit.getReason());
        vo.setCreateTime(outfit.getCreateTime());

        // 查询关联衣物
        List<OutfitItem> outfitItems = outfitItemMapper.selectList(
                new LambdaQueryWrapper<OutfitItem>().eq(OutfitItem::getOutfitId, outfit.getId()));
        List<WardrobeItemVO> itemVOs = new ArrayList<>();
        for (OutfitItem oi : outfitItems) {
            WardrobeItem wi = wardrobeItemMapper.selectById(oi.getWardrobeItemId());
            if (wi != null) {
                itemVOs.add(toWardrobeItemVO(wi));
            }
        }
        vo.setItems(itemVOs);

        return vo;
    }

    private WardrobeItemVO toWardrobeItemVO(WardrobeItem item) {
        WardrobeItemVO vo = new WardrobeItemVO();
        vo.setId(item.getId());
        vo.setUserId(item.getUserId());
        vo.setImageUrl(item.getImageUrl());
        vo.setThumbUrl(item.getThumbUrl());
        vo.setCategoryCode(item.getCategoryCode());
        vo.setSubType(item.getSubType());
        vo.setColor(item.getColor());
        vo.setStyle(item.getStyle());
        vo.setSeason(parseJsonList(item.getSeason()));
        vo.setOccasion(parseJsonList(item.getOccasion()));
        vo.setTags(parseJsonList(item.getAiTags()));
        vo.setAiRecognized(item.getAiRecognized() != null && item.getAiRecognized() == 1);
        vo.setFavorite(item.getFavorite() != null && item.getFavorite() == 1);
        vo.setWearCount(item.getWearCount());
        vo.setLastWearAt(item.getLastWearAt());
        vo.setCreateTime(item.getCreateTime());
        return vo;
    }

    private List<String> parseJsonList(String json) {
        if (StrUtil.isBlank(json) || "[]".equals(json)) {
            return new ArrayList<>();
        }
        try {
            return JSONUtil.toList(json, String.class);
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    private void checkAiEnabled() {
        if (!Boolean.TRUE.equals(aiEnabled)) {
            throw new BusinessException(503, "AI 功能暂未启用");
        }
    }

    private void checkDailyLimit(Long userId) {
        try {
            String key = "outfit:gen:" + userId + ":" + LocalDate.now().format(DATE_FORMAT);
            Long count = redisHelper.increment(key);
            if (count != null && count == 1) {
                redisHelper.expire(key, 2, TimeUnit.DAYS);
            }
            if (count != null && count > dailyLimit) {
                throw new BusinessException(429, "今日生成次数已达上限");
            }
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.warn("Redis 限流检查失败，放行", e);
        }
    }

    private JSONObject callTextAi(String prompt) {
        try {
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", aiModel);

            JSONArray messages = new JSONArray();
            JSONObject systemMsg = new JSONObject();
            systemMsg.set("role", "system");
            systemMsg.set("content",
                    "你是穿搭搭配专家。严格只输出 JSON：{\"itemIds\":[],\"reason\":\"搭配理由\",\"visualPrompt\":\"用于AI生图的英文描述\"}");
            messages.add(systemMsg);

            JSONObject userMsg = new JSONObject();
            userMsg.set("role", "user");
            userMsg.set("content", prompt);
            messages.add(userMsg);
            requestBody.put("messages", messages);
            requestBody.put("temperature", 0.7);

            String jsonBody = JSONUtil.toJsonStr(requestBody);
            HttpResponse response = HttpUtil.createPost(aiApiUrl)
                    .header("Authorization", "Bearer " + aiApiKey)
                    .header("Content-Type", "application/json")
                    .timeout(aiTimeout)
                    .body(jsonBody)
                    .execute();

            if (!response.isOk()) {
                log.error("AI 文本 API 调用失败: status={}, body={}", response.getStatus(), response.body());
                throw new BusinessException("AI 搭配生成失败");
            }

            JSONObject result = JSONUtil.parseObj(response.body());
            JSONArray choices = result.getJSONArray("choices");
            if (choices == null || choices.isEmpty()) {
                throw new BusinessException("AI 搭配生成失败");
            }

            String content = choices.getJSONObject(0).getJSONObject("message").getStr("content", "");
            // 剥离 markdown 代码块
            java.util.regex.Matcher matcher = java.util.regex.Pattern
                    .compile("```[a-zA-Z]*[\\s\\S]*?```").matcher(content);
            StringBuffer sb = new StringBuffer();
            while (matcher.find()) {
                String inner = matcher.group();
                int firstNewline = inner.indexOf('\n');
                int lastFence = inner.lastIndexOf("```");
                if (firstNewline >= 0 && lastFence > firstNewline) {
                    matcher.appendReplacement(sb, java.util.regex.Matcher.quoteReplacement(
                            inner.substring(firstNewline + 1, lastFence).trim()));
                } else {
                    matcher.appendReplacement(sb, java.util.regex.Matcher.quoteReplacement(inner));
                }
            }
            matcher.appendTail(sb);
            content = sb.toString().trim();
            int start = content.indexOf('{');
            int end = content.lastIndexOf('}');
            if (start >= 0 && end > start) {
                content = content.substring(start, end + 1);
            }

            return JSONUtil.parseObj(content);

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("AI 文本 API 调用异常", e);
            throw new BusinessException("AI 搭配生成失败");
        }
    }
}
