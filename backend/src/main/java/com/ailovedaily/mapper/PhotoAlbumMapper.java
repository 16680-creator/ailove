package com.ailovedaily.mapper;

import com.ailovedaily.entity.PhotoAlbum;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 相册Mapper接口
 */
@Mapper
public interface PhotoAlbumMapper extends BaseMapper<PhotoAlbum> {

    /**
     * 根据情侣ID查询相册列表
     */
    @Select("SELECT * FROM photo_album WHERE couple_id = #{coupleId} ORDER BY sort_order ASC, create_time DESC")
    List<PhotoAlbum> selectByCoupleId(@Param("coupleId") Long coupleId);

    /**
     * 更新照片数量
     */
    @Update("UPDATE photo_album SET photo_count = (SELECT COUNT(*) FROM photo WHERE album_id = #{albumId}) WHERE id = #{albumId}")
    int updatePhotoCount(@Param("albumId") Long albumId);
}
