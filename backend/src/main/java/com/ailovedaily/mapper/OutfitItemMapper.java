package com.ailovedaily.mapper;

import com.ailovedaily.entity.OutfitItem;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 穿搭方案衣物关联Mapper接口
 */
@Mapper
public interface OutfitItemMapper extends BaseMapper<OutfitItem> {
}
