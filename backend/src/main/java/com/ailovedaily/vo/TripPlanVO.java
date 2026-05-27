package com.ailovedaily.vo;

import lombok.Data;

import java.util.List;

/**
 * 旅行行程规划 VO
 */
@Data
public class TripPlanVO {

    /**
     * 行程标题，如"武汉3日浪漫之旅"
     */
    private String title;

    /**
     * 行程概览
     */
    private String summary;

    /**
     * 每日行程
     */
    private List<DayPlan> days;

    /**
     * 整体出行贴士
     */
    private List<String> tips;

    @Data
    public static class DayPlan {
        private String date;
        private String weekday;
        private String weather;
        private String weatherTip;
        private String tempRange;
        private List<PlanItem> items;
    }

    @Data
    public static class PlanItem {
        private String time;
        private String title;
        private String description;
        /**
         * 类型：景点/餐饮/交通/住宿/购物/休闲
         */
        private String type;
        private String tip;
    }
}
