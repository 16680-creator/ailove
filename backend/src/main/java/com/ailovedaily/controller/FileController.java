package com.ailovedaily.controller;

import com.ailovedaily.service.FileService;
import com.ailovedaily.vo.ResultVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * 文件控制器
 */
@RestController
@RequestMapping("/api/file")
@RequiredArgsConstructor
@Tag(name = "文件管理", description = "文件上传相关接口")
public class FileController {

    private final FileService fileService;

    @PostMapping("/upload")
    @Operation(summary = "上传文件", description = "通用文件上传接口")
    public ResultVO<String> uploadFile(@RequestParam("file") MultipartFile file,
                                       @RequestParam(defaultValue = "common") String folder) {
        String url = fileService.uploadFile(file, folder);
        return ResultVO.success(url);
    }

    @PostMapping("/upload/image")
    @Operation(summary = "上传图片", description = "上传图片并生成缩略图")
    public ResultVO<String[]> uploadImage(@RequestParam("file") MultipartFile file,
                                          @RequestParam(defaultValue = "images") String folder) {
        String[] urls = fileService.uploadImageWithThumbnail(file, folder);
        return ResultVO.success(urls);
    }
}
