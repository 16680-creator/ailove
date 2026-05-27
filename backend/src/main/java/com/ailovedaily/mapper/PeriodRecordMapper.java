package com.ailovedaily.mapper;

import com.ailovedaily.entity.PeriodRecord;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;
import java.util.List;

/**
 * 生理期记录Mapper接口
 */
@Mapper
public interface PeriodRecordMapper extends BaseMapper<PeriodRecord> {

    /**
     * 查询用户最近的生理期记录
     */
    @Select("SELECT * FROM period_record WHERE user_id = #{userId} AND is_predicted = 0 ORDER BY start_date DESC LIMIT 1")
    PeriodRecord selectLatestByUserId(@Param("userId") Long userId);

    /**
     * 查询用户所有实际记录
     */
    @Select("SELECT * FROM period_record WHERE user_id = #{userId} AND is_predicted = 0 ORDER BY start_date DESC")
    List<PeriodRecord> selectActualRecordsByUserId(@Param("userId") Long userId);

    /**
     * 查询需要提醒的记录（预计2天内开始）
     */
    @Select("SELECT * FROM period_record WHERE couple_id = #{coupleId} AND is_predicted = 1 AND start_date BETWEEN CURDATE() AND DATE_ADD(CURDATE(), INTERVAL 2 DAY)")
    List<PeriodRecord> selectUpcomingPeriods(@Param("coupleId") Long coupleId);

    /**
     * 删除某用户的所有预测记录
     */
    @Delete("DELETE FROM period_record WHERE user_id = #{userId} AND is_predicted = 1")
    int deletePredictedByUserId(@Param("userId") Long userId);

    /**
     * 查询某日期范围内的记录
     */
    @Select("SELECT * FROM period_record WHERE user_id = #{userId} AND start_date BETWEEN #{startDate} AND #{endDate} ORDER BY start_date")
    List<PeriodRecord> selectByDateRange(@Param("userId") Long userId, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);
}
