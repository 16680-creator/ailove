package com.ailovedaily.mapper;

import com.ailovedaily.entity.WardrobeItem;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 衣物Mapper接口
 */
@Mapper
public interface WardrobeItemMapper extends BaseMapper<WardrobeItem> {

    @Select("<script>" +
            "SELECT * FROM wardrobe_item WHERE user_id = #{userId} AND deleted = 0" +
            "<if test='category != null and category != \"\"'> AND category_code = #{category}</if>" +
            "<if test='season != null and season != \"\"'> AND season LIKE CONCAT('%', #{season}, '%')</if>" +
            " ORDER BY create_time DESC" +
            "</script>")
    List<WardrobeItem> selectByUserAndCategory(@Param("userId") Long userId,
                                                @Param("category") String category,
                                                @Param("season") String season);

    @Select("SELECT COUNT(*) FROM wardrobe_item WHERE user_id = #{userId} AND deleted = 0")
    Long countByUser(@Param("userId") Long userId);
}
