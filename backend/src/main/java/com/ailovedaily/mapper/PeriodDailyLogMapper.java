package com.ailovedaily.mapper;

import com.ailovedaily.entity.PeriodDailyLog;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;
import java.util.List;

/**
 * 经期每日打卡Mapper
 */
@Mapper
public interface PeriodDailyLogMapper extends BaseMapper<PeriodDailyLog> {

    /**
     * 查询某月所有日志
     */
    @Select("SELECT * FROM period_daily_log WHERE user_id = #{userId} AND YEAR(log_date) = #{year} AND MONTH(log_date) = #{month} ORDER BY log_date")
    List<PeriodDailyLog> selectByMonth(@Param("userId") Long userId, @Param("year") int year, @Param("month") int month);

    /**
     * 查询日期范围内的日志
     */
    @Select("SELECT * FROM period_daily_log WHERE user_id = #{userId} AND log_date BETWEEN #{startDate} AND #{endDate} ORDER BY log_date")
    List<PeriodDailyLog> selectByDateRange(@Param("userId") Long userId, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    /**
     * 查最近一次经期开始（is_period=1的最早连续日期）
     */
    @Select("SELECT * FROM period_daily_log WHERE user_id = #{userId} AND is_period = 1 ORDER BY log_date DESC LIMIT 1")
    PeriodDailyLog selectLatestPeriodDay(@Param("userId") Long userId);
}
