package com.ailovedaily.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.ailovedaily.entity.MenuItem;
import com.ailovedaily.mapper.MenuItemMapper;
import com.ailovedaily.service.DishRecipeService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executor;

/**
 * AI 菜品做法服务实现
 */
@Slf4j
@Service
public class DishRecipeServiceImpl implements DishRecipeService {

    private final MenuItemMapper menuItemMapper;
    private final Executor taskExecutor;

    public DishRecipeServiceImpl(MenuItemMapper menuItemMapper,
                                 @Qualifier("taskExecutor") Executor taskExecutor) {
        this.menuItemMapper = menuItemMapper;
        this.taskExecutor = taskExecutor;
    }

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

    @Override
    public String generateRecipe(Long menuItemId) {
        if (!Boolean.TRUE.equals(aiEnabled) || StrUtil.isBlank(aiApiKey)) {
            log.debug("AI 功能未启用，跳过做法生成");
            return null;
        }

        MenuItem menuItem = menuItemMapper.selectById(menuItemId);
        if (menuItem == null) {
            log.warn("菜品不存在: {}", menuItemId);
            return null;
        }

        String recipe = callAiApi(menuItem);
        if (StrUtil.isNotBlank(recipe)) {
            menuItem.setRecipe(recipe);
            menuItemMapper.updateById(menuItem);
            log.info("菜品做法生成成功: menuItemId={}, name={}", menuItemId, menuItem.getName());
        }
        return recipe;
    }

    @Override
    public void generateRecipeAsync(Long menuItemId) {
        taskExecutor.execute(() -> {
            try {
                generateRecipe(menuItemId);
            } catch (Exception e) {
                log.error("异步生成菜品做法失败: menuItemId={}", menuItemId, e);
            }
        });
    }

    private String callAiApi(MenuItem menuItem) {
        String prompt = buildPrompt(menuItem);

        try {
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", aiModel);

            JSONArray messages = new JSONArray();
            JSONObject systemMsg = new JSONObject();
            systemMsg.set("role", "system");
            systemMsg.set("content", "你是一位经验丰富的家常菜厨师。直接输出做法步骤，不要有任何解释、标题、标记或英文。使用中文回答。");
            messages.add(systemMsg);

            JSONObject userMsg = new JSONObject();
            userMsg.set("role", "user");
            userMsg.set("content", prompt);
            messages.add(userMsg);
            requestBody.put("messages", messages);

            requestBody.put("temperature", 0.7);
            requestBody.put("max_tokens", 2048);

            String jsonBody = JSONUtil.toJsonStr(requestBody);
            log.debug("AI API 请求 (菜品做法): {}", jsonBody);

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

            // MiniMax base_resp 错误检查
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

            // M2.7 思维链模型兼容
            if (StrUtil.isBlank(content)) {
                content = respMessage.getStr("reasoning_content", "");
            }

            content = cleanRecipe(content);
            return content;

        } catch (Exception e) {
            log.error("AI API 调用异常", e);
            return null;
        }
    }

    private String buildPrompt(MenuItem menuItem) {
        String categoryNames = "家常菜,西餐,小吃,甜品,饮品";
        String categoryName = menuItem.getCategory() != null && menuItem.getCategory() >= 1 && menuItem.getCategory() <= 5
                ? categoryNames.split(",")[menuItem.getCategory() - 1] : "未知";

        StringBuilder sb = new StringBuilder();
        sb.append("请为以下菜品给出详细的做法：\n\n");
        sb.append("菜品名称：").append(menuItem.getName()).append("\n");
        sb.append("分类：").append(categoryName).append("\n");
        if (menuItem.getDifficulty() != null) {
            sb.append("难度：").append(menuItem.getDifficulty()).append("星\n");
        }
        if (menuItem.getCookTime() != null) {
            sb.append("烹饪时间：约").append(menuItem.getCookTime()).append("分钟\n");
        }
        if (StrUtil.isNotBlank(menuItem.getDescription())) {
            sb.append("描述：").append(menuItem.getDescription()).append("\n");
        }
        sb.append("\n要求：\n");
        sb.append("1. 先列出所需食材清单（含用量）\n");
        sb.append("2. 再给出分步做法，步骤清晰编号\n");
        sb.append("3. 最后给出1-2条小贴士\n");
        sb.append("4. 内容简洁实用，控制在500字以内\n");
        sb.append("5. 只输出做法内容，不要加标题或解释");
        return sb.toString();
    }

    private String cleanRecipe(String content) {
        if (StrUtil.isBlank(content)) {
            return "";
        }
        // 去除思维链标签残留
        content = content.replaceAll("<think[^>]*>[\\s\\S]*?</think\\s*>", "");
        content = content.replaceAll("</?think[^>]*>", "");
        // 去除 markdown 代码块标记
        content = content.replaceAll("```[a-zA-Z]*\\n?", "");
        // 去除常见前缀
        content = content.replaceAll("^(做法[:：]?|菜谱[:：]?|食谱[:：]?|回复[:：]?|答案[:：]?|内容[:：]?)\\s*", "");
        return content.trim();
    }
}
