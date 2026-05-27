package com.ailovedaily.mapper;

import com.ailovedaily.entity.Diary;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 日记Mapper接口
 */
@Mapper
public interface DiaryMapper extends BaseMapper<Diary> {

    /**
     * 分页查询日记列表
     */
    @Select("SELECT * FROM diary WHERE couple_id = #{coupleId} ORDER BY diary_date DESC, create_time DESC")
    List<Diary> selectPageByCoupleId(Page<Diary> page, @Param("coupleId") Long coupleId);

    /**
     * 查询时间轴数据
     */
    @Select("SELECT * FROM diary WHERE couple_id = #{coupleId} ORDER BY diary_date DESC LIMIT #{limit}")
    List<Diary> selectTimelineByCoupleId(@Param("coupleId") Long coupleId, @Param("limit") Integer limit);

    /**
     * 增加浏览次数
     */
    @Update("UPDATE diary SET view_count = view_count + 1 WHERE id = #{id}")
    int incrementViewCount(@Param("id") Long id);

    /**
     * 切换收藏状态
     */
    @Update("UPDATE diary SET is_favorite = 1 - is_favorite WHERE id = #{id}")
    int toggleFavorite(@Param("id") Long id);
}
