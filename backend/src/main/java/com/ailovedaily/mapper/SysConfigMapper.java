package com.ailovedaily.mapper;

import com.ailovedaily.entity.SysConfig;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 系统配置Mapper接口
 */
@Mapper
public interface SysConfigMapper extends BaseMapper<SysConfig> {

    /**
     * 根据配置键查询
     */
    @Select("SELECT * FROM sys_config WHERE config_key = #{configKey} LIMIT 1")
    SysConfig selectByKey(@Param("configKey") String configKey);

    /**
     * 根据配置键获取值
     */
    @Select("SELECT config_value FROM sys_config WHERE config_key = #{configKey} LIMIT 1")
    String selectValueByKey(@Param("configKey") String configKey);
}
