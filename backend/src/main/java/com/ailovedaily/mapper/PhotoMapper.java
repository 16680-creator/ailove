package com.ailovedaily.mapper;

import com.ailovedaily.entity.Photo;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 照片Mapper接口
 */
@Mapper
public interface PhotoMapper extends BaseMapper<Photo> {

    /**
     * 分页查询相册照片
     */
    @Select("SELECT * FROM photo WHERE album_id = #{albumId} ORDER BY create_time DESC")
    List<Photo> selectPageByAlbumId(Page<Photo> page, @Param("albumId") Long albumId);

    /**
     * 根据情侣ID查询所有照片（瀑布流）
     */
    @Select("SELECT * FROM photo WHERE couple_id = #{coupleId} ORDER BY create_time DESC LIMIT #{limit}")
    List<Photo> selectByCoupleId(@Param("coupleId") Long coupleId, @Param("limit") Integer limit);

    /**
     * 切换收藏状态
     */
    @Update("UPDATE photo SET is_favorite = 1 - is_favorite WHERE id = #{id}")
    int toggleFavorite(@Param("id") Long id);

    /**
     * 查询收藏的照片
     */
    @Select("SELECT * FROM photo WHERE couple_id = #{coupleId} AND is_favorite = 1 ORDER BY create_time DESC")
    List<Photo> selectFavoritesByCoupleId(@Param("coupleId") Long coupleId);
}
