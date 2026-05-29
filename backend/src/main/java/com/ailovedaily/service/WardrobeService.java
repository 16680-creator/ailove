package com.ailovedaily.service;

import com.ailovedaily.dto.WardrobeItemUpdateDTO;
import com.ailovedaily.vo.WardrobeItemVO;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 衣物管理服务接口
 */
public interface WardrobeService {

    WardrobeItemVO upload(MultipartFile file, Long userId);

    List<WardrobeItemVO> list(Long userId, String category, String season, Boolean partnerView);

    WardrobeItemVO detail(Long id, Long userId);

    WardrobeItemVO update(Long id, Long userId, WardrobeItemUpdateDTO dto);

    Boolean delete(Long id, Long userId);

    Boolean favorite(Long id, Long userId);

    WardrobeItemVO recognize(Long id, Long userId);
}
