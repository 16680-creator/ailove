package com.ailovedaily.service;

import com.ailovedaily.dto.OutfitGenerateDTO;
import com.ailovedaily.vo.OutfitVO;

import java.util.List;

/**
 * 穿搭方案服务接口
 */
public interface OutfitService {

    OutfitVO autoMatch(Long userId, OutfitGenerateDTO dto);

    OutfitVO manualGenerate(Long userId, OutfitGenerateDTO dto);

    List<OutfitVO> list(Long userId, int pageNum, int pageSize);

    OutfitVO detail(Long id, Long userId);

    Boolean delete(Long id, Long userId);
}
