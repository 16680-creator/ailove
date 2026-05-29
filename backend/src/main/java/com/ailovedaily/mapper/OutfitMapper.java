package com.ailovedaily.mapper;

import com.ailovedaily.entity.Outfit;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 穿搭方案Mapper接口
 */
@Mapper
public interface OutfitMapper extends BaseMapper<Outfit> {

    @Select("SELECT * FROM outfit WHERE user_id = #{userId} AND deleted = 0 ORDER BY create_time DESC LIMIT #{offset}, #{limit}")
    List<Outfit> selectByUserOrderByCreateTime(@Param("userId") Long userId,
                                               @Param("offset") int offset,
                                               @Param("limit") int limit);
}
