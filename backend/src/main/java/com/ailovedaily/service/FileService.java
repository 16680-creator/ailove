package com.ailovedaily.service;

import org.springframework.web.multipart.MultipartFile;

/**
 * 文件服务接口
 */
public interface FileService {

    /**
     * 上传文件
     *
     * @param file       文件
     * @param folder     文件夹名称
     * @return 文件访问URL
     */
    String uploadFile(MultipartFile file, String folder);

    /**
     * 上传图片并生成缩略图
     *
     * @param file   图片文件
     * @param folder 文件夹名称
     * @return [原图URL, 缩略图URL]
     */
    String[] uploadImageWithThumbnail(MultipartFile file, String folder);

    /**
     * 删除文件
     *
     * @param fileUrl 文件URL
     */
    void deleteFile(String fileUrl);

    /**
     * 获取文件完整路径
     *
     * @param fileUrl 文件URL
     * @return 完整路径
     */
    String getFullPath(String fileUrl);
}
