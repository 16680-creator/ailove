package com.ailovedaily.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.img.ImgUtil;
import cn.hutool.core.io.FileUtil;
import com.ailovedaily.entity.Photo;
import com.ailovedaily.entity.PhotoAlbum;
import com.ailovedaily.entity.User;
import com.ailovedaily.mapper.PhotoAlbumMapper;
import com.ailovedaily.mapper.PhotoMapper;
import com.ailovedaily.mapper.UserMapper;
import com.ailovedaily.service.AlbumService;
import com.ailovedaily.service.FileService;
import com.ailovedaily.vo.PhotoVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 相册服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AlbumServiceImpl implements AlbumService {

    private final PhotoAlbumMapper photoAlbumMapper;
    private final PhotoMapper photoMapper;
    private final UserMapper userMapper;
    private final FileService fileService;

    @Override
    public PhotoAlbum createAlbum(Long userId, String name, String description, String coverUrl) {
        User user = userMapper.selectById(userId);
        if (user == null || user.getCoupleId() == null) {
            throw new RuntimeException("用户未绑定情侣关系");
        }

        PhotoAlbum album = new PhotoAlbum();
        album.setCoupleId(user.getCoupleId());
        album.setName(name);
        album.setDescription(description);
        album.setCoverUrl(coverUrl);
        album.setCreateBy(userId);
        album.setPhotoCount(0);

        photoAlbumMapper.insert(album);
        return album;
    }

    @Override
    public void updateAlbum(Long id, String name, String description) {
        PhotoAlbum album = new PhotoAlbum();
        album.setId(id);
        album.setName(name);
        album.setDescription(description);
        photoAlbumMapper.updateById(album);
    }

    @Override
    public void deleteAlbum(Long id) {
        // 删除相册下的所有照片
        LambdaQueryWrapper<Photo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Photo::getAlbumId, id);
        List<Photo> photos = photoMapper.selectList(wrapper);

        for (Photo photo : photos) {
            fileService.deleteFile(photo.getUrl());
            if (photo.getThumbnailUrl() != null) {
                fileService.deleteFile(photo.getThumbnailUrl());
            }
        }

        photoMapper.delete(wrapper);
        photoAlbumMapper.deleteById(id);
    }

    @Override
    public List<PhotoAlbum> getAlbumList(Long coupleId) {
        return photoAlbumMapper.selectByCoupleId(coupleId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PhotoVO uploadPhoto(Long userId, Long albumId, MultipartFile file, String description) {
        User user = userMapper.selectById(userId);
        if (user == null || user.getCoupleId() == null) {
            throw new RuntimeException("用户未绑定情侣关系");
        }

        // 上传图片并生成缩略图
        String[] urls = fileService.uploadImageWithThumbnail(file, "photos");
        String originalUrl = urls[0];
        String thumbnailUrl = urls[1];

        // 获取图片尺寸
        int width = 0, height = 0;
        try {
            BufferedImage image = ImageIO.read(file.getInputStream());
            if (image != null) {
                width = image.getWidth();
                height = image.getHeight();
            }
        } catch (Exception e) {
            log.warn("获取图片尺寸失败", e);
        }

        // 保存照片记录
        Photo photo = new Photo();
        photo.setAlbumId(albumId);
        photo.setCoupleId(user.getCoupleId());
        photo.setUserId(userId);
        photo.setUrl(originalUrl);
        photo.setThumbnailUrl(thumbnailUrl);
        photo.setDescription(description);
        photo.setFileSize(file.getSize());
        photo.setWidth(width);
        photo.setHeight(height);

        photoMapper.insert(photo);

        // 更新相册照片数量
        photoAlbumMapper.updatePhotoCount(albumId);

        // 更新相册封面（如果是第一张）
        PhotoAlbum album = photoAlbumMapper.selectById(albumId);
        if (album != null && album.getCoverUrl() == null) {
            album.setCoverUrl(thumbnailUrl);
            photoAlbumMapper.updateById(album);
        }

        return convertToVO(photo);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<PhotoVO> uploadPhotos(Long userId, Long albumId, List<MultipartFile> files) {
        List<PhotoVO> result = new ArrayList<>();
        for (MultipartFile file : files) {
            result.add(uploadPhoto(userId, albumId, file, null));
        }
        return result;
    }

    @Override
    public void deletePhoto(Long id) {
        Photo photo = photoMapper.selectById(id);
        if (photo == null) {
            return;
        }

        // 删除文件
        fileService.deleteFile(photo.getUrl());
        if (photo.getThumbnailUrl() != null) {
            fileService.deleteFile(photo.getThumbnailUrl());
        }

        photoMapper.deleteById(id);

        // 更新相册照片数量
        photoAlbumMapper.updatePhotoCount(photo.getAlbumId());
    }

    @Override
    public Page<PhotoVO> getPhotoPage(Long albumId, Integer page, Integer size) {
        Page<Photo> photoPage = new Page<>(page, size);
        LambdaQueryWrapper<Photo> wrapper = new LambdaQueryWrapper<Photo>()
                .eq(Photo::getAlbumId, albumId)
                .orderByDesc(Photo::getCreateTime);
        photoMapper.selectPage(photoPage, wrapper);

        List<PhotoVO> voList = photoPage.getRecords().stream().map(this::convertToVO).collect(Collectors.toList());

        Page<PhotoVO> resultPage = new Page<>();
        BeanUtil.copyProperties(photoPage, resultPage);
        resultPage.setRecords(voList);

        return resultPage;
    }

    @Override
    public List<PhotoVO> getAllPhotos(Long coupleId, Integer limit) {
        List<Photo> photos = photoMapper.selectByCoupleId(coupleId, limit);
        return photos.stream().map(this::convertToVO).collect(Collectors.toList());
    }

    @Override
    public void toggleFavorite(Long id) {
        photoMapper.toggleFavorite(id);
    }

    @Override
    public List<PhotoVO> getFavoritePhotos(Long coupleId) {
        List<Photo> photos = photoMapper.selectFavoritesByCoupleId(coupleId);
        return photos.stream().map(this::convertToVO).collect(Collectors.toList());
    }

    @Override
    public PhotoVO getPhotoDetail(Long id) {
        Photo photo = photoMapper.selectById(id);
        if (photo == null) {
            return null;
        }
        return convertToVO(photo);
    }

    private PhotoVO convertToVO(Photo photo) {
        PhotoVO vo = new PhotoVO();
        BeanUtil.copyProperties(photo, vo);

        // 获取上传者信息
        User uploader = userMapper.selectById(photo.getUserId());
        if (uploader != null) {
            vo.setUserNickname(uploader.getNickname());
        }

        // 获取相册信息
        PhotoAlbum album = photoAlbumMapper.selectById(photo.getAlbumId());
        if (album != null) {
            vo.setAlbumName(album.getName());
        }

        return vo;
    }
}
