package com.ailovedaily.mapper;

import com.ailovedaily.entity.MenuItem;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 菜品Mapper接口
 */
@Mapper
public interface MenuItemMapper extends BaseMapper<MenuItem> {

    /**
     * 根据情侣ID和分类查询
     */
    @Select("SELECT * FROM menu_item WHERE couple_id = #{coupleId} AND (#{category} IS NULL OR category = #{category}) ORDER BY create_time DESC")
    List<MenuItem> selectByCoupleIdAndCategory(@Param("coupleId") Long coupleId, @Param("category") Integer category);

    /**
     * 随机获取一个菜品
     */
    @Select("SELECT * FROM menu_item WHERE couple_id = #{coupleId} ORDER BY RAND() LIMIT 1")
    MenuItem selectRandomByCoupleId(@Param("coupleId") Long coupleId);
}
