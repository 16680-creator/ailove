package com.ailovedaily.controller;

import com.ailovedaily.entity.PhotoAlbum;
import com.ailovedaily.service.AlbumService;
import com.ailovedaily.vo.PhotoVO;
import com.ailovedaily.vo.ResultVO;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 相册控制器
 */
@RestController
@RequestMapping("/api/album")
@RequiredArgsConstructor
@Tag(name = "生活相册", description = "相册和照片管理相关接口")
public class AlbumController {

    private final AlbumService albumService;

    // ==================== 相册管理 ====================

    @PostMapping
    @Operation(summary = "创建相册", description = "创建新的相册")
    public ResultVO<PhotoAlbum> createAlbum(@RequestAttribute("userId") Long userId,
                                            @RequestBody PhotoAlbum albumDTO) {
        PhotoAlbum album = albumService.createAlbum(userId, albumDTO.getName(), albumDTO.getDescription(), albumDTO.getCoverUrl());
        return ResultVO.success(album);
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新相册", description = "更新相册信息")
    public ResultVO<Void> updateAlbum(@PathVariable Long id,
                                      @RequestBody PhotoAlbum albumDTO) {
        albumService.updateAlbum(id, albumDTO.getName(), albumDTO.getDescription());
        return ResultVO.success();
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除相册", description = "删除相册及其所有照片")
    public ResultVO<Void> deleteAlbum(@PathVariable Long id) {
        albumService.deleteAlbum(id);
        return ResultVO.success();
    }

    @GetMapping("/list")
    @Operation(summary = "获取相册列表", description = "获取所有相册")
    public ResultVO<List<PhotoAlbum>> getAlbumList(@RequestAttribute("coupleId") Long coupleId) {
        List<PhotoAlbum> albums = albumService.getAlbumList(coupleId);
        return ResultVO.success(albums);
    }

    // ==================== 照片管理 ====================

    @PostMapping("/{albumId}/photo")
    @Operation(summary = "上传照片", description = "上传单张照片到指定相册")
    public ResultVO<PhotoVO> uploadPhoto(@RequestAttribute("userId") Long userId,
                                         @PathVariable Long albumId,
                                         @RequestParam("file") MultipartFile file,
                                         @RequestParam(required = false) String description) {
        PhotoVO photo = albumService.uploadPhoto(userId, albumId, file, description);
        return ResultVO.success(photo);
    }

    @PostMapping("/{albumId}/photos")
    @Operation(summary = "批量上传照片", description = "批量上传多张照片")
    public ResultVO<List<PhotoVO>> uploadPhotos(@RequestAttribute("userId") Long userId,
                                                @PathVariable Long albumId,
                                                @RequestParam("files") List<MultipartFile> files) {
        List<PhotoVO> photos = albumService.uploadPhotos(userId, albumId, files);
        return ResultVO.success(photos);
    }

    @DeleteMapping("/photo/{id}")
    @Operation(summary = "删除照片", description = "删除指定照片")
    public ResultVO<Void> deletePhoto(@PathVariable Long id) {
        albumService.deletePhoto(id);
        return ResultVO.success();
    }

    @GetMapping("/{albumId}/photos")
    @Operation(summary = "分页查询相册照片", description = "分页获取相册中的照片")
    public ResultVO<Page<PhotoVO>> getPhotoPage(@PathVariable Long albumId,
                                                @RequestParam(defaultValue = "1") Integer page,
                                                @RequestParam(defaultValue = "20") Integer size) {
        Page<PhotoVO> photoPage = albumService.getPhotoPage(albumId, page, size);
        return ResultVO.success(photoPage);
    }

    @GetMapping("/photos/all")
    @Operation(summary = "获取所有照片", description = "获取所有照片（瀑布流展示）")
    public ResultVO<List<PhotoVO>> getAllPhotos(@RequestAttribute("coupleId") Long coupleId,
                                                @RequestParam(defaultValue = "50") Integer limit) {
        List<PhotoVO> photos = albumService.getAllPhotos(coupleId, limit);
        return ResultVO.success(photos);
    }

    @PostMapping("/photo/{id}/favorite")
    @Operation(summary = "切换收藏状态", description = "收藏或取消收藏照片")
    public ResultVO<Void> toggleFavorite(@PathVariable Long id) {
        albumService.toggleFavorite(id);
        return ResultVO.success();
    }

    @GetMapping("/photos/favorites")
    @Operation(summary = "获取收藏的照片", description = "获取所有收藏的照片")
    public ResultVO<List<PhotoVO>> getFavoritePhotos(@RequestAttribute("coupleId") Long coupleId) {
        List<PhotoVO> photos = albumService.getFavoritePhotos(coupleId);
        return ResultVO.success(photos);
    }

    @GetMapping("/photo/{id}")
    @Operation(summary = "获取照片详情", description = "获取指定照片的详细信息")
    public ResultVO<PhotoVO> getPhotoDetail(@PathVariable Long id) {
        PhotoVO photo = albumService.getPhotoDetail(id);
        return ResultVO.success(photo);
    }
}
