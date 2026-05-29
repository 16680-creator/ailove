package com.ailovedaily.service.impl;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.ailovedaily.exception.BusinessException;
import com.ailovedaily.service.AiImageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * AI图像生成服务实现
 */
@Slf4j
@Service
public class AiImageServiceImpl implements AiImageService {

    @Value("${ai.api-key:}")
    private String aiApiKey;

    @Value("${ai.timeout:300000}")
    private Integer aiTimeout;

    @Value("${ai.image.api-url:}")
    private String imageApiUrl;

    @Value("${ai.image.model:image-01}")
    private String imageModel;

    @Value("${ai.image.aspect-ratio:3:4}")
    private String aspectRatio;

    @Value("${file.upload.path:${user.dir}/uploads}")
    private String uploadPath;

    @Value("${file.access.url:/uploads}")
    private String accessUrl;

    @Override
    public String generateOutfitImage(String prompt, List<String> referenceImages) {
        try {
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", imageModel);
            requestBody.put("prompt", prompt);
            requestBody.put("aspect_ratio", aspectRatio);
            requestBody.put("n", 1);
            requestBody.put("response_format", "url");

            String jsonBody = JSONUtil.toJsonStr(requestBody);
            log.debug("AI Image API 请求: {}", jsonBody);

            HttpResponse response = HttpUtil.createPost(imageApiUrl)
                    .header("Authorization", "Bearer " + aiApiKey)
                    .header("Content-Type", "application/json")
                    .timeout(aiTimeout)
                    .body(jsonBody)
                    .execute();

            if (!response.isOk()) {
                log.error("AI Image API 调用失败: status={}, body={}", response.getStatus(), response.body());
                throw new BusinessException("AI 图像生成失败");
            }

            JSONObject result = JSONUtil.parseObj(response.body());
            JSONArray imageData = result.getJSONArray("data");
            if (imageData == null || imageData.isEmpty()) {
                log.error("AI Image API 返回为空: {}", response.body());
                throw new BusinessException("AI 图像生成失败");
            }

            String generatedUrl = imageData.getJSONObject(0).getStr("image_url", "");
            if (StrUtil.isBlank(generatedUrl)) {
                generatedUrl = imageData.getJSONObject(0).getStr("url", "");
            }
            if (StrUtil.isBlank(generatedUrl)) {
                throw new BusinessException("AI 图像生成失败：未返回图片URL");
            }

            // 下载图片到本地
            return downloadAndSave(generatedUrl);

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("AI Image API 调用异常", e);
            throw new BusinessException("AI 图像生成失败");
        }
    }

    private String downloadAndSave(String imageUrl) {
        try {
            String datePath = DateUtil.today().replace("-", "/");
            String filename = IdUtil.fastSimpleUUID() + ".jpg";
            String relativePath = "outfit/" + datePath + "/" + filename;

            Path uploadRootPath = Paths.get(uploadPath).toAbsolutePath().normalize();
            Path targetPath = uploadRootPath.resolve(relativePath).normalize();
            FileUtil.mkdir(targetPath.getParent());

            HttpResponse response = HttpUtil.createGet(imageUrl)
                    .timeout(aiTimeout)
                    .execute();

            if (!response.isOk()) {
                throw new BusinessException("下载AI生成图片失败");
            }

            FileUtil.writeBytes(response.bodyBytes(), targetPath.toFile());
            log.info("AI 生成穿搭图片已保存: {}", targetPath);

            return accessUrl + "/" + relativePath;

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("保存AI生成图片失败", e);
            throw new BusinessException("保存AI生成图片失败");
        }
    }
}
