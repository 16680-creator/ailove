package com.ailovedaily.service.impl;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.img.ImgUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.ailovedaily.service.FileService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.PostConstruct;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 文件服务实现。
 */
@Slf4j
@Service
public class FileServiceImpl implements FileService {

    @Value("${file.upload.path:/app/uploads}")
    private String uploadPath;

    @Value("${file.access.url:/uploads}")
    private String accessUrl;

    private Path uploadRootPath;

    @PostConstruct
    public void init() {
        uploadRootPath = resolveUploadRootPath();
        createDirectories(uploadRootPath);
        createDirectories(uploadRootPath.resolve("thumbnails"));
    }

    @Override
    public String uploadFile(MultipartFile file, String folder) {
        try {
            String originalFilename = file.getOriginalFilename();
            String extension = FileUtil.extName(originalFilename);
            String newFilename = IdUtil.fastSimpleUUID() + (StrUtil.isBlank(extension) ? "" : "." + extension);

            String datePath = DateUtil.today().replace("-", "/");
            String relativePath = folder + "/" + datePath + "/" + newFilename;
            Path targetPath = uploadRootPath.resolve(relativePath).normalize();

            createDirectories(targetPath.getParent());
            file.transferTo(targetPath.toFile());

            log.info("文件上传成功: {}", targetPath);
            return accessUrl + "/" + relativePath;
        } catch (IOException exception) {
            log.error("文件上传失败", exception);
            throw new RuntimeException("文件上传失败");
        }
    }

    @Override
    public String[] uploadImageWithThumbnail(MultipartFile file, String folder) {
        try {
            String originalUrl = uploadFile(file, folder);

            String originalFilename = file.getOriginalFilename();
            String extension = FileUtil.extName(originalFilename);
            String thumbnailFilename = IdUtil.fastSimpleUUID() + "_thumb" +
                    (StrUtil.isBlank(extension) ? "" : "." + extension);

            String datePath = DateUtil.today().replace("-", "/");
            String thumbnailRelativePath = "thumbnails/" + folder + "/" + datePath + "/" + thumbnailFilename;
            Path thumbnailPath = uploadRootPath.resolve(thumbnailRelativePath).normalize();
            Path originalPath = resolveStoredFilePath(originalUrl);

            createDirectories(thumbnailPath.getParent());

            BufferedImage image = ImgUtil.read(originalPath.toFile());
            if (image != null) {
                int width = image.getWidth();
                int height = image.getHeight();

                if (width > 800) {
                    int newHeight = (int) ((800.0 / width) * height);
                    ImgUtil.scale(originalPath.toFile(), thumbnailPath.toFile(), 800, newHeight, null);
                } else {
                    FileUtil.copy(originalPath.toFile(), thumbnailPath.toFile(), true);
                }
            }

            String thumbnailUrl = accessUrl + "/" + thumbnailRelativePath;
            return new String[]{originalUrl, thumbnailUrl};
        } catch (Exception exception) {
            log.error("图片上传失败", exception);
            throw new RuntimeException("图片上传失败");
        }
    }

    @Override
    public void deleteFile(String fileUrl) {
        if (StrUtil.isBlank(fileUrl)) {
            return;
        }

        try {
            Path fullPath = resolveStoredFilePath(fileUrl);
            FileUtil.del(fullPath.toFile());
            log.info("文件删除成功: {}", fullPath);
        } catch (Exception exception) {
            log.error("文件删除失败: {}", fileUrl, exception);
        }
    }

    @Override
    public String getFullPath(String fileUrl) {
        if (StrUtil.isBlank(fileUrl)) {
            return null;
        }
        return resolveStoredFilePath(fileUrl).toString();
    }

    private Path resolveUploadRootPath() {
        Path configuredPath = Paths.get(uploadPath);
        if (!configuredPath.isAbsolute()) {
            configuredPath = Paths.get(System.getProperty("user.dir")).resolve(configuredPath);
        }
        return configuredPath.toAbsolutePath().normalize();
    }

    private Path resolveStoredFilePath(String fileUrl) {
        String relativePath = fileUrl.replace(accessUrl, "");
        if (relativePath.startsWith("/")) {
            relativePath = relativePath.substring(1);
        }
        return uploadRootPath.resolve(relativePath).normalize();
    }

    private void createDirectories(Path path) {
        if (path == null) {
            return;
        }

        try {
            Files.createDirectories(path);
        } catch (IOException exception) {
            throw new RuntimeException("创建上传目录失败: " + path, exception);
        }
    }
}
