package com.ailovedaily.service;

import com.ailovedaily.entity.PhotoAlbum;
import com.ailovedaily.vo.PhotoVO;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 相册服务接口
 */
public interface AlbumService {

    /**
     * 创建相册
     */
    PhotoAlbum createAlbum(Long userId, String name, String description, String coverUrl);

    /**
     * 更新相册
     */
    void updateAlbum(Long id, String name, String description);

    /**
     * 删除相册
     */
    void deleteAlbum(Long id);

    /**
     * 获取相册列表
     */
    List<PhotoAlbum> getAlbumList(Long coupleId);

    /**
     * 上传照片
     */
    PhotoVO uploadPhoto(Long userId, Long albumId, MultipartFile file, String description);

    /**
     * 批量上传照片
     */
    List<PhotoVO> uploadPhotos(Long userId, Long albumId, List<MultipartFile> files);

    /**
     * 删除照片
     */
    void deletePhoto(Long id);

    /**
     * 分页查询相册照片
     */
    Page<PhotoVO> getPhotoPage(Long albumId, Integer page, Integer size);

    /**
     * 获取所有照片（瀑布流）
     */
    List<PhotoVO> getAllPhotos(Long coupleId, Integer limit);

    /**
     * 切换收藏状态
     */
    void toggleFavorite(Long id);

    /**
     * 获取收藏的照片
     */
    List<PhotoVO> getFavoritePhotos(Long coupleId);

    /**
     * 获取照片详情
     */
    PhotoVO getPhotoDetail(Long id);
}
