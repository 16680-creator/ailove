package com.ailovedaily.mapper;

import com.ailovedaily.entity.DailyQuote;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/**
 * 每日一言Mapper接口
 */
@Mapper
public interface DailyQuoteMapper extends BaseMapper<DailyQuote> {

    /**
     * 随机获取一条情话
     */
    @Select("SELECT * FROM daily_quote WHERE category = #{category} ORDER BY RAND() LIMIT 1")
    DailyQuote selectRandomByCategory(@Param("category") Integer category);

    /**
     * 增加使用次数
     */
    @Update("UPDATE daily_quote SET use_count = use_count + 1 WHERE id = #{id}")
    int incrementUseCount(@Param("id") Long id);

    /**
     * 查询指定情侣当天的AI情话
     */
    @Select("SELECT * FROM daily_quote WHERE couple_id = #{coupleId} AND quote_date = #{quoteDate} AND source = 1 LIMIT 1")
    DailyQuote selectByCoupleAndDate(@Param("coupleId") Long coupleId, @Param("quoteDate") String quoteDate);

    /**
     * 查询指定情侣最近的AI情话
     */
    @Select("SELECT * FROM daily_quote WHERE couple_id = #{coupleId} AND source = 1 ORDER BY quote_date DESC LIMIT 1")
    DailyQuote selectLatestByCouple(@Param("coupleId") Long coupleId);
}
