package com.ailovedaily.mapper;

import com.ailovedaily.entity.TripPlan;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface TripPlanMapper extends BaseMapper<TripPlan> {

    @Select("SELECT * FROM trip_plan WHERE user_id = #{userId} ORDER BY create_time DESC")
    List<TripPlan> selectByUserId(@Param("userId") Long userId);
}
