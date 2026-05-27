package com.ailovedaily.service.impl;

import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.ailovedaily.service.WeatherService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * 天气服务实现 — 使用 wttr.in 免费 API
 */
@Slf4j
@Service
public class WeatherServiceImpl implements WeatherService {

    private static final String WTTR_URL = "https://wttr.in/%s?format=j1&lang=zh";
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @Override
    public List<WeatherDay> getWeather(String city, LocalDate fromDate, LocalDate toDate) {
        List<WeatherDay> result = new ArrayList<>();
        try {
            String encodedCity = URLEncoder.encode(city, StandardCharsets.UTF_8.name());
            String url = String.format(WTTR_URL, encodedCity);
            log.info("请求天气: {}", url);

            HttpResponse resp = HttpUtil.createGet(url)
                    .header("Accept-Language", "zh-CN")
                    .timeout(10000)
                    .execute();

            if (!resp.isOk()) {
                log.warn("天气 API 返回异常: status={}", resp.getStatus());
                return result;
            }

            JSONObject root = JSONUtil.parseObj(resp.body());
            JSONArray forecast = root.getJSONArray("weather");
            if (forecast == null || forecast.isEmpty()) {
                log.warn("天气 API 无预报数据");
                return result;
            }

            for (int i = 0; i < forecast.size(); i++) {
                JSONObject day = forecast.getJSONObject(i);
                String dateStr = day.getStr("date", "");
                LocalDate date = LocalDate.parse(dateStr, FMT);

                // 只取日期范围内的（wttr.in 只返回3天预报，所以放宽范围）
                if (date.isBefore(fromDate.minusDays(1)) || date.isAfter(toDate.plusDays(1))) continue;

                String mintC = day.getStr("mintC", "");
                String maxtC = day.getStr("maxtC", "");

                // 取中午时段的天气描述
                String weatherDesc = "";
                String windDir = "";
                String windSpeed = "";
                String humidity = "";
                JSONArray hourly = day.getJSONArray("hourly");
                if (hourly != null && hourly.size() > 4) {
                    JSONObject noon = hourly.getJSONObject(4); // index 4 ≈ 12:00
                    JSONArray descArr = noon.getJSONArray("lang_zh");
                    if (descArr != null && !descArr.isEmpty()) {
                        weatherDesc = descArr.getJSONObject(0).getStr("value", "");
                    }
                    if (weatherDesc.isEmpty()) {
                        weatherDesc = noon.getStr("weatherDesc", "");
                    }
                    windDir = noon.getStr("winddir16Point", "");
                    windSpeed = noon.getStr("windspeedKmph", "");
                    humidity = noon.getStr("humidity", "");
                }

                WeatherDay wd = new WeatherDay();
                wd.date = dateStr;
                wd.weather = weatherDesc.isEmpty() ? "未知" : weatherDesc;
                wd.tempMin = mintC;
                wd.tempMax = maxtC;
                wd.windDir = windDir;
                wd.windScale = windSpeed;
                wd.humidity = humidity;
                wd.tip = buildTip(weatherDesc, mintC, maxtC);

                result.add(wd);
            }

            log.info("获取到 {} 天天气数据", result.size());
        } catch (Exception e) {
            log.error("获取天气失败: city={}", city, e);
        }
        return result;
    }

    private String buildTip(String weather, String min, String max) {
        if (weather.contains("雨")) {
            return "记得带伞";
        } else if (weather.contains("雪")) {
            return "注意保暖防滑";
        } else if (weather.contains("晴")) {
            return "注意防晒";
        }
        try {
            int low = Integer.parseInt(min);
            if (low < 10) return "注意保暖";
            if (low > 30) return "注意防暑";
        } catch (Exception ignored) {}
        return "";
    }
}
