package com.ailovedaily.mapper;

import com.ailovedaily.entity.CoupleLink;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/**
 * 情侣关系Mapper接口
 */
@Mapper
public interface CoupleLinkMapper extends BaseMapper<CoupleLink> {

    /**
     * 根据邀请码查询
     */
    @Select("SELECT * FROM couple_link WHERE invite_code = #{inviteCode} LIMIT 1")
    CoupleLink selectByInviteCode(@Param("inviteCode") String inviteCode);

    /**
     * 更新绑定状态
     */
    @Update("UPDATE couple_link SET user2_id = #{user2Id}, status = 1, bind_time = NOW() WHERE id = #{id}")
    int updateBindStatus(@Param("id") Long id, @Param("user2Id") Long user2Id);
}
