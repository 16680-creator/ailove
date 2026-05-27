package com.ailovedaily.mapper;

import com.ailovedaily.entity.WishList;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 心愿清单Mapper接口
 */
@Mapper
public interface WishListMapper extends BaseMapper<WishList> {

    /**
     * 根据情侣ID和状态查询
     */
    @Select("SELECT * FROM wish_list WHERE couple_id = #{coupleId} AND (#{status} IS NULL OR status = #{status}) ORDER BY priority DESC, create_time DESC")
    List<WishList> selectByCoupleIdAndStatus(@Param("coupleId") Long coupleId, @Param("status") Integer status);

    /**
     * 根据分类查询
     */
    @Select("SELECT * FROM wish_list WHERE couple_id = #{coupleId} AND category = #{category} ORDER BY create_time DESC")
    List<WishList> selectByCategory(@Param("coupleId") Long coupleId, @Param("category") Integer category);

    /**
     * 更新心愿状态为已完成
     */
    @Update("UPDATE wish_list SET status = 2, complete_date = CURDATE(), complete_by = #{userId} WHERE id = #{id}")
    int completeWish(@Param("id") Long id, @Param("userId") Long userId);

    /**
     * 统计各状态数量
     */
    @Select("SELECT status, COUNT(*) as count FROM wish_list WHERE couple_id = #{coupleId} GROUP BY status")
    List<java.util.Map<String, Object>> countByStatus(@Param("coupleId") Long coupleId);
}
