package com.ailovedaily.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.ailovedaily.dto.WardrobeItemUpdateDTO;
import com.ailovedaily.entity.User;
import com.ailovedaily.entity.WardrobeCategory;
import com.ailovedaily.entity.WardrobeItem;
import com.ailovedaily.exception.BusinessException;
import com.ailovedaily.mapper.UserMapper;
import com.ailovedaily.mapper.WardrobeCategoryMapper;
import com.ailovedaily.mapper.WardrobeItemMapper;
import com.ailovedaily.service.AiVisionService;
import com.ailovedaily.service.FileService;
import com.ailovedaily.service.WardrobeService;
import com.ailovedaily.vo.AiRecognizeResultVO;
import com.ailovedaily.vo.WardrobeItemVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 衣物管理服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WardrobeServiceImpl implements WardrobeService {

    private final WardrobeItemMapper wardrobeItemMapper;
    private final WardrobeCategoryMapper wardrobeCategoryMapper;
    private final UserMapper userMapper;
    private final FileService fileService;
    private final AiVisionService aiVisionService;

    @Value("${ai.enabled:false}")
    private Boolean aiEnabled;

    @Override
    public WardrobeItemVO upload(MultipartFile file, Long userId) {
        // 上传图片并生成缩略图
        String[] urls = fileService.uploadImageWithThumbnail(file, "wardrobe");
        String imageUrl = urls[0];
        String thumbUrl = urls[1];

        // 组装实体，先用默认分类入库，立即返回
        WardrobeItem item = new WardrobeItem();
        item.setUserId(userId);

        User user = userMapper.selectById(userId);
        if (user != null) {
            item.setCoupleId(user.getCoupleId());
        }

        item.setImageUrl(imageUrl);
        item.setThumbUrl(thumbUrl);
        item.setCategoryCode("top");
        item.setAiRecognized(0);
        item.setFavorite(0);
        item.setWearCount(0);

        wardrobeItemMapper.insert(item);
        Long itemId = item.getId();

        // AI 识别在后台异步执行，不阻塞上传
        if (Boolean.TRUE.equals(aiEnabled)) {
            CompletableFuture.runAsync(() -> {
                try {
                    AiRecognizeResultVO result = aiVisionService.recognizeClothing(imageUrl);
                    if (result != null && Boolean.TRUE.equals(result.getSuccess())) {
                        WardrobeItem updateItem = wardrobeItemMapper.selectById(itemId);
                        if (updateItem != null) {
                            updateItem.setCategoryCode(StrUtil.blankToDefault(result.getCategory(), "top"));
                            updateItem.setSubType(result.getSubType());
                            updateItem.setColor(result.getColor());
                            updateItem.setStyle(result.getStyle());
                            updateItem.setSeason(toJsonArray(result.getSeason()));
                            updateItem.setOccasion(toJsonArray(result.getOccasion()));
                            updateItem.setAiTags(toJsonArray(result.getTags()));
                            updateItem.setAiRecognized(1);
                            wardrobeItemMapper.updateById(updateItem);
                            log.info("AI 衣物识别完成: itemId={}", itemId);
                        }
                    }
                } catch (Exception e) {
                    log.warn("AI 衣物识别失败: itemId={}", itemId, e);
                }
            });
        }

        return toVO(item);
    }

    @Override
    public List<WardrobeItemVO> list(Long userId, String category, String season, Boolean partnerView) {
        Long queryUserId = userId;
        if (Boolean.TRUE.equals(partnerView)) {
            User user = userMapper.selectById(userId);
            if (user == null || user.getCoupleId() == null) {
                return new ArrayList<>();
            }
            User partner = userMapper.selectPartnerByCoupleId(user.getCoupleId(), userId);
            if (partner == null) {
                return new ArrayList<>();
            }
            queryUserId = partner.getId();
        }

        List<WardrobeItem> items = wardrobeItemMapper.selectByUserAndCategory(queryUserId, category, season);
        return items.stream().map(this::toVO).collect(Collectors.toList());
    }

    @Override
    public WardrobeItemVO detail(Long id, Long userId) {
        WardrobeItem item = wardrobeItemMapper.selectById(id);
        if (item == null) {
            throw new BusinessException("衣物不存在");
        }
        if (!item.getUserId().equals(userId)) {
            throw new BusinessException(403, "无权操作");
        }
        return toVO(item);
    }

    @Override
    public WardrobeItemVO update(Long id, Long userId, WardrobeItemUpdateDTO dto) {
        WardrobeItem item = wardrobeItemMapper.selectById(id);
        if (item == null) {
            throw new BusinessException("衣物不存在");
        }
        if (!item.getUserId().equals(userId)) {
            throw new BusinessException(403, "无权操作");
        }

        if (StrUtil.isNotBlank(dto.getCategoryCode())) {
            item.setCategoryCode(dto.getCategoryCode());
        }
        if (dto.getSubType() != null) {
            item.setSubType(dto.getSubType());
        }
        if (dto.getColor() != null) {
            item.setColor(dto.getColor());
        }
        if (dto.getStyle() != null) {
            item.setStyle(dto.getStyle());
        }
        if (dto.getSeason() != null) {
            item.setSeason(toJsonArray(dto.getSeason()));
        }
        if (dto.getOccasion() != null) {
            item.setOccasion(toJsonArray(dto.getOccasion()));
        }
        if (dto.getTags() != null) {
            item.setAiTags(toJsonArray(dto.getTags()));
        }

        wardrobeItemMapper.updateById(item);
        return toVO(item);
    }

    @Override
    public Boolean delete(Long id, Long userId) {
        WardrobeItem item = wardrobeItemMapper.selectById(id);
        if (item == null) {
            throw new BusinessException("衣物不存在");
        }
        if (!item.getUserId().equals(userId)) {
            throw new BusinessException(403, "无权操作");
        }
        wardrobeItemMapper.deleteById(id);
        return true;
    }

    @Override
    public Boolean favorite(Long id, Long userId) {
        WardrobeItem item = wardrobeItemMapper.selectById(id);
        if (item == null) {
            throw new BusinessException("衣物不存在");
        }
        if (!item.getUserId().equals(userId)) {
            throw new BusinessException(403, "无权操作");
        }
        item.setFavorite(item.getFavorite() != null && item.getFavorite() == 1 ? 0 : 1);
        wardrobeItemMapper.updateById(item);
        return item.getFavorite() == 1;
    }

    @Override
    public WardrobeItemVO recognize(Long id, Long userId) {
        WardrobeItem item = wardrobeItemMapper.selectById(id);
        if (item == null) {
            throw new BusinessException("衣物不存在");
        }
        if (!item.getUserId().equals(userId)) {
            throw new BusinessException(403, "无权操作");
        }

        if (!Boolean.TRUE.equals(aiEnabled)) {
            throw new BusinessException(503, "AI 功能暂未启用");
        }

        AiRecognizeResultVO result = aiVisionService.recognizeClothing(item.getImageUrl());
        if (result != null && Boolean.TRUE.equals(result.getSuccess())) {
            item.setCategoryCode(StrUtil.blankToDefault(result.getCategory(), "top"));
            item.setSubType(result.getSubType());
            item.setColor(result.getColor());
            item.setStyle(result.getStyle());
            item.setSeason(toJsonArray(result.getSeason()));
            item.setOccasion(toJsonArray(result.getOccasion()));
            item.setAiTags(toJsonArray(result.getTags()));
            item.setAiRecognized(1);
            wardrobeItemMapper.updateById(item);
        }

        return toVO(item);
    }

    private WardrobeItemVO toVO(WardrobeItem item) {
        WardrobeItemVO vo = new WardrobeItemVO();
        vo.setId(item.getId());
        vo.setUserId(item.getUserId());
        vo.setImageUrl(item.getImageUrl());
        vo.setThumbUrl(item.getThumbUrl());
        vo.setCategoryCode(item.getCategoryCode());
        vo.setCategoryName(resolveCategoryName(item.getCategoryCode()));
        vo.setSubType(item.getSubType());
        vo.setColor(item.getColor());
        vo.setStyle(item.getStyle());
        vo.setSeason(fromJsonArray(item.getSeason()));
        vo.setOccasion(fromJsonArray(item.getOccasion()));
        vo.setTags(fromJsonArray(item.getAiTags()));
        vo.setAiRecognized(item.getAiRecognized() != null && item.getAiRecognized() == 1);
        vo.setFavorite(item.getFavorite() != null && item.getFavorite() == 1);
        vo.setWearCount(item.getWearCount());
        vo.setLastWearAt(item.getLastWearAt());
        vo.setCreateTime(item.getCreateTime());
        return vo;
    }

    private String resolveCategoryName(String code) {
        if (StrUtil.isBlank(code)) {
            return "上衣";
        }
        WardrobeCategory cat = wardrobeCategoryMapper.selectOne(
                new LambdaQueryWrapper<WardrobeCategory>().eq(WardrobeCategory::getCode, code));
        return cat != null ? cat.getName() : "上衣";
    }

    private String toJsonArray(List<String> list) {
        if (list == null || list.isEmpty()) {
            return "[]";
        }
        return JSONUtil.toJsonStr(list);
    }

    private List<String> fromJsonArray(String json) {
        if (StrUtil.isBlank(json) || "[]".equals(json)) {
            return new ArrayList<>();
        }
        try {
            return JSONUtil.toList(json, String.class);
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }
}
