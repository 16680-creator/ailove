package com.ailovedaily.mapper;

import com.ailovedaily.entity.MealRecord;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;
import java.util.List;

/**
 * 餐食记录Mapper接口
 */
@Mapper
public interface MealRecordMapper extends BaseMapper<MealRecord> {

    /**
     * 查询指定couple最近N天有记录的所有日期
     */
    @Select("SELECT DISTINCT meal_date FROM meal_record WHERE couple_id = #{coupleId} AND meal_date >= #{startDate} ORDER BY meal_date DESC")
    List<LocalDate> selectDistinctDates(@Param("coupleId") Long coupleId, @Param("startDate") LocalDate startDate);

    /**
     * 按coupleId和日期查询记录
     */
    @Select("SELECT * FROM meal_record WHERE couple_id = #{coupleId} AND meal_date = #{mealDate} ORDER BY create_time DESC")
    List<MealRecord> selectByCoupleIdAndDate(@Param("coupleId") Long coupleId, @Param("mealDate") LocalDate mealDate);
}
