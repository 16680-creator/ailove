package com.ailovedaily.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.ailovedaily.dto.TripPlanDTO;
import com.ailovedaily.entity.CoupleLink;
import com.ailovedaily.entity.TripPlan;
import com.ailovedaily.entity.User;
import com.ailovedaily.mapper.CoupleLinkMapper;
import com.ailovedaily.mapper.TripPlanMapper;
import com.ailovedaily.mapper.UserMapper;
import com.ailovedaily.service.TripPlanService;
import com.ailovedaily.service.WeatherService;
import com.ailovedaily.service.WeatherService.WeatherDay;
import com.ailovedaily.vo.TripPlanVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.Executor;

@Slf4j
@Service
public class TripPlanServiceImpl implements TripPlanService {

    private final WeatherService weatherService;
    private final UserMapper userMapper;
    private final CoupleLinkMapper coupleLinkMapper;
    private final TripPlanMapper tripPlanMapper;
    private final Executor taskExecutor;

    public TripPlanServiceImpl(WeatherService weatherService, UserMapper userMapper,
                               CoupleLinkMapper coupleLinkMapper, TripPlanMapper tripPlanMapper,
                               @Qualifier("taskExecutor") Executor taskExecutor) {
        this.weatherService = weatherService;
        this.userMapper = userMapper;
        this.coupleLinkMapper = coupleLinkMapper;
        this.tripPlanMapper = tripPlanMapper;
        this.taskExecutor = taskExecutor;
    }

    @Value("${ai.api-url:}")
    private String aiApiUrl;

    @Value("${ai.api-key:}")
    private String aiApiKey;

    @Value("${ai.model:deepseek-chat}")
    private String aiModel;

    @Value("${ai.timeout:60000}")
    private Integer aiTimeout;

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @Override
    public Long startGenerate(TripPlanDTO dto, Long userId) {
        User user = userMapper.selectById(userId);
        Long coupleId = user != null ? user.getCoupleId() : null;

        TripPlan plan = new TripPlan();
        plan.setUserId(userId);
        plan.setCoupleId(coupleId);
        plan.setFromCity(dto.getFromCity());
        plan.setToCity(dto.getToCity());
        plan.setStartDate(dto.getStartDate());
        plan.setEndDate(dto.getEndDate());
        plan.setPreferences(dto.getPreferences());
        plan.setBudget(dto.getBudget());
        plan.setCustomRequest(dto.getCustomRequest());
        plan.setStatus(0);
        tripPlanMapper.insert(plan);

        Long planId = plan.getId();
        taskExecutor.execute(() -> doGenerate(planId));
        return planId;
    }

    private void doGenerate(Long planId) {
        TripPlan plan = tripPlanMapper.selectById(planId);
        if (plan == null) return;
        try {
            LocalDate startDate = LocalDate.parse(plan.getStartDate(), FMT);
            LocalDate endDate = LocalDate.parse(plan.getEndDate(), FMT);

            List<WeatherDay> weatherDays = weatherService.getWeather(plan.getToCity(), startDate, endDate);
            String weatherSummary = buildWeatherSummary(weatherDays);
            String coupleContext = buildCoupleContext(plan.getUserId());

            TripPlanDTO dto = new TripPlanDTO();
            dto.setFromCity(plan.getFromCity());
            dto.setToCity(plan.getToCity());
            dto.setStartDate(plan.getStartDate());
            dto.setEndDate(plan.getEndDate());
            dto.setPreferences(plan.getPreferences());
            dto.setBudget(plan.getBudget());
            dto.setCustomRequest(plan.getCustomRequest());

            long days = ChronoUnit.DAYS.between(startDate, endDate) + 1;
            String prompt = buildPrompt(dto, days, weatherSummary, coupleContext);
            String aiResponse = callAi(prompt);

            TripPlanVO vo;
            if (StrUtil.isBlank(aiResponse)) {
                vo = buildFallback(dto, weatherDays, days);
            } else {
                vo = parseAiResponse(aiResponse, dto, weatherDays, days);
            }

            plan.setResultJson(JSONUtil.toJsonStr(vo));
            plan.setStatus(1);
            tripPlanMapper.updateById(plan);
            log.info("行程生成成功: id={}", plan.getId());

        } catch (Exception e) {
            log.error("行程生成失败: id={}", plan.getId(), e);
            plan.setStatus(2);
            plan.setErrorMsg(e.getMessage());
            tripPlanMapper.updateById(plan);
        }
    }

    @Override
    public TripPlan getById(Long id) {
        return tripPlanMapper.selectById(id);
    }

    @Override
    public List<TripPlan> getUserPlans(Long userId) {
        return tripPlanMapper.selectByUserId(userId);
    }

    @Override
    public void delete(Long id, Long userId) {
        TripPlan plan = tripPlanMapper.selectById(id);
        if (plan != null && plan.getUserId().equals(userId)) {
            tripPlanMapper.deleteById(id);
        }
    }

    // ---- 以下为原有辅助方法 ----

    private String buildWeatherSummary(List<WeatherDay> weatherDays) {
        if (weatherDays.isEmpty()) {
            return "暂无天气预报数据，请提醒用户出发前关注天气。";
        }
        StringBuilder sb = new StringBuilder();
        for (WeatherDay wd : weatherDays) {
            sb.append(wd.date).append(": ")
              .append(wd.weather).append("，")
              .append(wd.tempMin).append("~").append(wd.tempMax).append("°C");
            if (StrUtil.isNotBlank(wd.tip)) {
                sb.append("（").append(wd.tip).append("）");
            }
            sb.append("；");
        }
        return sb.toString();
    }

    private String buildCoupleContext(Long userId) {
        try {
            User user = userMapper.selectById(userId);
            if (user == null || user.getCoupleId() == null) return "";

            CoupleLink couple = coupleLinkMapper.selectById(user.getCoupleId());
            if (couple == null) return "";

            long daysTogether = ChronoUnit.DAYS.between(couple.getLoveStartDate(), LocalDate.now());
            User partner = null;
            if (couple.getUser1Id().equals(userId)) {
                partner = userMapper.selectById(couple.getUser2Id());
            } else {
                partner = userMapper.selectById(couple.getUser1Id());
            }

            String nick1 = StrUtil.isNotBlank(user.getNickname()) ? user.getNickname() : "TA";
            String nick2 = partner != null && StrUtil.isNotBlank(partner.getNickname()) ? partner.getNickname() : "TA";

            return String.format("这是一对在一起%d天的情侣（%s和%s），请为他们规划温馨浪漫的行程。",
                    daysTogether, nick1, nick2);
        } catch (Exception e) {
            log.warn("获取情侣信息失败", e);
            return "";
        }
    }

    private String buildPrompt(TripPlanDTO dto, long days, String weatherSummary, String coupleContext) {
        StringBuilder sb = new StringBuilder();
        sb.append("规划行程，直接返回JSON。\n");
        sb.append(dto.getFromCity()).append("→").append(dto.getToCity());
        sb.append("，").append(dto.getStartDate()).append("~").append(dto.getEndDate());
        sb.append("，共").append(days).append("天");

        if (StrUtil.isNotBlank(dto.getPreferences())) {
            sb.append("，偏好：").append(dto.getPreferences());
        }
        if (StrUtil.isNotBlank(dto.getBudget())) {
            sb.append("，预算：").append(dto.getBudget());
        }
        if (StrUtil.isNotBlank(dto.getCustomRequest())) {
            sb.append("\n用户额外要求：").append(dto.getCustomRequest());
        }

        sb.append("\n天气：").append(weatherSummary);

        if (StrUtil.isNotBlank(coupleContext)) {
            sb.append("\n").append(coupleContext);
        }

        sb.append("\n\nJSON格式：\n");
        sb.append("{\"title\":\"标题\",\"summary\":\"概览\",\"days\":[{\"date\":\"日期\",\"weekday\":\"周几\",\"weather\":\"天气\",\"weatherTip\":\"建议\",\"tempRange\":\"温度\",\"items\":[{\"time\":\"09:00\",\"title\":\"活动\",\"description\":\"描述\",\"type\":\"景点\",\"tip\":\"贴士\"}]}],\"tips\":[\"贴士\"]}\n");
        sb.append("\n注意事项：\n");
        sb.append("1. days 数组必须包含完整的 ").append(days).append(" 天（").append(dto.getStartDate()).append(" 到 ").append(dto.getEndDate()).append("），一天都不能少\n");
        sb.append("2. type 只能是：景点/餐饮/交通/住宿/购物/休闲\n");
        sb.append("3. 每天安排 4-6 个活动，时间从早到晚排列\n");
        sb.append("4. 结合天气情况给出合理建议（如下雨天安排室内活动）\n");
        sb.append("5. 包含交通方式建议（从出发城市到目的地的交通）\n");
        sb.append("6. 第一天和最后一天考虑往返交通时间\n");
        sb.append("7. tips 给出 3-5 条实用出行建议\n");
        sb.append("只返回JSON，不要有其他内容。");

        return sb.toString();
    }

    private String callAi(String prompt) {
        if (StrUtil.isBlank(aiApiKey)) {
            log.warn("AI API Key 未配置");
            return null;
        }
        java.net.HttpURLConnection conn = null;
        try {
            Map<String, Object> body = new HashMap<>();
            body.put("model", aiModel);

            JSONArray messages = new JSONArray();
            JSONObject sysMsg = new JSONObject();
            sysMsg.set("role", "system");
            sysMsg.set("content", "你是旅行规划师。直接返回JSON格式的行程安排，不要有任何解释或推理过程。只输出JSON。");
            messages.add(sysMsg);
            JSONObject msg = new JSONObject();
            msg.set("role", "user");
            msg.set("content", prompt);
            messages.add(msg);
            body.put("messages", messages);
            body.put("temperature", 0.7);
            body.put("max_tokens", 16000);

            String jsonBody = JSONUtil.toJsonStr(body);
            log.info("调用 AI API 生成行程, model={}", aiModel);

            java.net.URL url = new java.net.URL(aiApiUrl);
            conn = (java.net.HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Authorization", "Bearer " + aiApiKey);
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);
            conn.setConnectTimeout(15000);
            conn.setReadTimeout(aiTimeout);

            try (java.io.OutputStream os = conn.getOutputStream()) {
                os.write(jsonBody.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            }

            int status = conn.getResponseCode();
            if (status != 200) {
                java.io.InputStream errStream = conn.getErrorStream();
                String errBody = "";
                if (errStream != null) {
                    java.io.BufferedReader ebr = new java.io.BufferedReader(
                            new java.io.InputStreamReader(errStream, java.nio.charset.StandardCharsets.UTF_8));
                    StringBuilder esb = new StringBuilder();
                    String eline;
                    while ((eline = ebr.readLine()) != null) {
                        esb.append(eline);
                    }
                    ebr.close();
                    errBody = esb.toString();
                }
                log.error("AI API 调用失败: status={}, body={}", status, errBody);
                return null;
            }

            java.io.BufferedReader br = new java.io.BufferedReader(
                    new java.io.InputStreamReader(conn.getInputStream(), java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }
            br.close();

            JSONObject result = JSONUtil.parseObj(sb.toString());
            JSONArray choices = result.getJSONArray("choices");
            if (choices == null || choices.isEmpty()) {
                return null;
            }

            JSONObject message = choices.getJSONObject(0).getJSONObject("message");
            String content = message.getStr("content", "");
            if (StrUtil.isBlank(content)) {
                content = message.getStr("reasoning_content", "");
            }
            log.info("AI API 调用成功, content长度={}", content.length());
            return content;
        } catch (Exception e) {
            log.error("AI API 调用异常: {}", e.getMessage());
            return null;
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    private TripPlanVO parseAiResponse(String aiResponse, TripPlanDTO dto, List<WeatherDay> weatherDays, long days) {
        try {
            String json = aiResponse.trim();
            if (json.startsWith("```")) {
                json = json.replaceAll("^```(json)?\\s*", "").replaceAll("\\s*```$", "");
            }
            int start = json.indexOf('{');
            int end = json.lastIndexOf('}');
            if (start >= 0 && end > start) {
                json = json.substring(start, end + 1);
            }

            JSONObject root = JSONUtil.parseObj(json);
            TripPlanVO vo = new TripPlanVO();
            vo.setTitle(root.getStr("title", dto.getToCity() + days + "日之旅"));
            vo.setSummary(root.getStr("summary", ""));

            JSONArray daysArr = root.getJSONArray("days");
            if (daysArr != null) {
                List<TripPlanVO.DayPlan> dayPlans = new ArrayList<>();
                for (int i = 0; i < daysArr.size(); i++) {
                    JSONObject dayObj = daysArr.getJSONObject(i);
                    TripPlanVO.DayPlan dp = new TripPlanVO.DayPlan();
                    dp.setDate(dayObj.getStr("date", ""));
                    dp.setWeekday(dayObj.getStr("weekday", ""));
                    dp.setWeather(dayObj.getStr("weather", ""));
                    dp.setWeatherTip(dayObj.getStr("weatherTip", ""));
                    dp.setTempRange(dayObj.getStr("tempRange", ""));

                    JSONArray itemsArr = dayObj.getJSONArray("items");
                    if (itemsArr != null) {
                        List<TripPlanVO.PlanItem> items = new ArrayList<>();
                        for (int j = 0; j < itemsArr.size(); j++) {
                            JSONObject itemObj = itemsArr.getJSONObject(j);
                            TripPlanVO.PlanItem pi = new TripPlanVO.PlanItem();
                            pi.setTime(itemObj.getStr("time", ""));
                            pi.setTitle(itemObj.getStr("title", ""));
                            pi.setDescription(itemObj.getStr("description", ""));
                            pi.setType(itemObj.getStr("type", "景点"));
                            pi.setTip(itemObj.getStr("tip", ""));
                            items.add(pi);
                        }
                        dp.setItems(items);
                    }
                    dayPlans.add(dp);
                }
                vo.setDays(dayPlans);
            }

            JSONArray tipsArr = root.getJSONArray("tips");
            if (tipsArr != null) {
                List<String> tips = new ArrayList<>();
                for (int i = 0; i < tipsArr.size(); i++) {
                    tips.add(tipsArr.getStr(i));
                }
                vo.setTips(tips);
            }

            return vo;
        } catch (Exception e) {
            log.error("解析 AI 返回失败, raw={}", aiResponse, e);
            return buildFallback(dto, weatherDays, days);
        }
    }

    private TripPlanVO buildFallback(TripPlanDTO dto, List<WeatherDay> weatherDays, long days) {
        TripPlanVO vo = new TripPlanVO();
        vo.setTitle(dto.getToCity() + days + "日之旅");
        vo.setSummary("AI 暂时无法生成行程，请稍后重试。以下是天气信息供参考。");

        List<TripPlanVO.DayPlan> dayPlans = new ArrayList<>();
        for (WeatherDay wd : weatherDays) {
            TripPlanVO.DayPlan dp = new TripPlanVO.DayPlan();
            dp.setDate(wd.date);
            dp.setWeather(wd.weather);
            dp.setTempRange(wd.tempMin + "~" + wd.tempMax + "°C");
            dp.setWeatherTip(wd.tip);
            dp.setItems(new ArrayList<>());
            dayPlans.add(dp);
        }
        vo.setDays(dayPlans);
        vo.setTips(Arrays.asList("请关注目的地天气变化", "提前预订住宿和交通", "准备舒适的步行鞋"));

        return vo;
    }
}
