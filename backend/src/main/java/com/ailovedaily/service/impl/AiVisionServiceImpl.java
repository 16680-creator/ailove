package com.ailovedaily.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.ailovedaily.service.AiVisionService;
import com.ailovedaily.vo.AiRecognizeResultVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * AI视觉识别服务实现
 */
@Slf4j
@Service
public class AiVisionServiceImpl implements AiVisionService {

    @Value("${ai.enabled:false}")
    private Boolean aiEnabled;

    @Value("${ai.api-key:}")
    private String aiApiKey;

    @Value("${ai.timeout:300000}")
    private Integer aiTimeout;

    @Value("${ai.vision.api-url:}")
    private String visionApiUrl;

    @Value("${ai.vision.model:abab-vision-pro}")
    private String visionModel;

    @Value("${ai.vision.timeout:30000}")
    private Integer visionTimeout;

    @Override
    public AiRecognizeResultVO recognizeClothing(String imageUrl) {
        if (!Boolean.TRUE.equals(aiEnabled) || StrUtil.isBlank(aiApiKey)) {
            log.debug("AI 功能未启用，跳过衣物识别");
            return buildDefaultResult();
        }

        // 失败重试1次
        for (int attempt = 0; attempt < 2; attempt++) {
            try {
                AiRecognizeResultVO result = callVisionApi(imageUrl);
                if (result != null && Boolean.TRUE.equals(result.getSuccess())) {
                    return result;
                }
                log.warn("AI 视觉识别失败，第{}次尝试", attempt + 1);
            } catch (Exception e) {
                log.error("AI 视觉识别异常，第{}次尝试", attempt + 1, e);
            }
        }

        return buildDefaultResult();
    }

    private AiRecognizeResultVO callVisionApi(String imageUrl) {
        try {
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", visionModel);

            JSONArray messages = new JSONArray();

            JSONObject systemMsg = new JSONObject();
            systemMsg.set("role", "system");
            systemMsg.set("content",
                    "你是衣物识别专家。严格只输出 JSON：{\"category\":\"top\",\"subType\":\"T恤\",\"color\":\"白色\","
                    + "\"style\":\"简约\",\"season\":[\"spring\",\"summer\"],\"occasion\":[\"daily\"],\"tags\":[\"纯棉\",\"圆领\"]}");
            messages.add(systemMsg);

            JSONObject userMsg = new JSONObject();
            userMsg.set("role", "user");
            JSONArray content = new JSONArray();
            JSONObject textPart = new JSONObject();
            textPart.set("type", "text");
            textPart.set("text", "识别这件衣物");
            content.add(textPart);
            JSONObject imagePart = new JSONObject();
            imagePart.set("type", "image_url");
            JSONObject imageUrlObj = new JSONObject();
            imageUrlObj.set("url", imageUrl);
            imagePart.set("image_url", imageUrlObj);
            content.add(imagePart);
            userMsg.set("content", content);
            messages.add(userMsg);

            requestBody.put("messages", messages);
            requestBody.put("temperature", 0.3);

            String jsonBody = JSONUtil.toJsonStr(requestBody);
            log.debug("AI Vision API 请求: {}", jsonBody);

            HttpResponse response = HttpUtil.createPost(visionApiUrl)
                    .header("Authorization", "Bearer " + aiApiKey)
                    .header("Content-Type", "application/json")
                    .timeout(visionTimeout)
                    .body(jsonBody)
                    .execute();

            if (!response.isOk()) {
                log.error("AI Vision API 调用失败: status={}, body={}", response.getStatus(), response.body());
                return null;
            }

            JSONObject result = JSONUtil.parseObj(response.body());
            JSONArray choices = result.getJSONArray("choices");
            if (choices == null || choices.isEmpty()) {
                log.error("AI Vision API 返回为空: {}", response.body());
                return null;
            }

            String contentStr = choices.getJSONObject(0).getJSONObject("message").getStr("content", "");
            contentStr = cleanJsonContent(contentStr);

            JSONObject parsed = JSONUtil.parseObj(contentStr);
            AiRecognizeResultVO vo = new AiRecognizeResultVO();
            vo.setSuccess(true);
            vo.setCategory(parsed.getStr("category", "top"));
            vo.setSubType(parsed.getStr("subType", ""));
            vo.setColor(parsed.getStr("color", ""));
            vo.setStyle(parsed.getStr("style", ""));
            vo.setSeason(parseJsonArray(parsed, "season"));
            vo.setOccasion(parseJsonArray(parsed, "occasion"));
            vo.setTags(parseJsonArray(parsed, "tags"));
            return vo;

        } catch (Exception e) {
            log.error("AI Vision API 调用异常", e);
            return null;
        }
    }

    private String cleanJsonContent(String content) {
        if (StrUtil.isBlank(content)) {
            return "{}";
        }
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
        // 如果不是以 { 开头，尝试找到第一个 { 和最后一个 }
        int start = content.indexOf('{');
        int end = content.lastIndexOf('}');
        if (start >= 0 && end > start) {
            content = content.substring(start, end + 1);
        }
        return content;
    }

    private List<String> parseJsonArray(JSONObject obj, String key) {
        JSONArray arr = obj.getJSONArray(key);
        if (arr == null) {
            return new ArrayList<>();
        }
        List<String> list = new ArrayList<>();
        for (int i = 0; i < arr.size(); i++) {
            list.add(arr.getStr(i, ""));
        }
        return list;
    }

    private AiRecognizeResultVO buildDefaultResult() {
        AiRecognizeResultVO vo = new AiRecognizeResultVO();
        vo.setSuccess(false);
        vo.setCategory("top");
        vo.setSubType("");
        vo.setColor("");
        vo.setStyle("");
        vo.setSeason(new ArrayList<>());
        vo.setOccasion(new ArrayList<>());
        vo.setTags(new ArrayList<>());
        return vo;
    }
}
