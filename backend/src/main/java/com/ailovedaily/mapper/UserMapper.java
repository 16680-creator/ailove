package com.ailovedaily.mapper;

import com.ailovedaily.entity.User;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 用户Mapper接口
 */
@Mapper
public interface UserMapper extends BaseMapper<User> {

    /**
     * 根据OpenID查询用户
     */
    @Select("SELECT * FROM sys_user WHERE openid = #{openid} LIMIT 1")
    User selectByOpenid(@Param("openid") String openid);

    /**
     * 根据登录账号查询用户。
     */
    @Select("SELECT * FROM sys_user WHERE login_name = #{loginName} LIMIT 1")
    User selectByLoginName(@Param("loginName") String loginName);

    /**
     * 根据情侣ID查询伴侣
     */
    @Select("SELECT * FROM sys_user WHERE couple_id = #{coupleId} AND id != #{userId} LIMIT 1")
    User selectPartnerByCoupleId(@Param("coupleId") Long coupleId, @Param("userId") Long userId);

    /**
     * 根据情侣ID查询所有用户
     */
    @Select("SELECT * FROM sys_user WHERE couple_id = #{coupleId}")
    List<User> selectByCoupleId(@Param("coupleId") Long coupleId);
}
