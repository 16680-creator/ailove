package com.ailovedaily.service;

import java.time.LocalDate;
import java.util.List;

/**
 * 天气查询服务
 */
public interface WeatherService {

    /**
     * 获取指定城市未来几天的天气预报
     *
     * @param city     城市名（中文）
     * @param fromDate 开始日期
     * @param toDate   结束日期
     * @return 每日天气信息列表
     */
    List<WeatherDay> getWeather(String city, LocalDate fromDate, LocalDate toDate);

    class WeatherDay {
        public String date;
        public String weather;
        public String tempMin;
        public String tempMax;
        public String windDir;
        public String windScale;
        public String humidity;
        public String tip;

        public WeatherDay() {}

        public WeatherDay(String date, String weather, String tempMin, String tempMax) {
            this.date = date;
            this.weather = weather;
            this.tempMin = tempMin;
            this.tempMax = tempMax;
        }
    }
}
