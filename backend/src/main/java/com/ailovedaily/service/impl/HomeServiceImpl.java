package com.ailovedaily.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.ailovedaily.entity.CoupleLink;
import com.ailovedaily.entity.DailyQuote;
import com.ailovedaily.entity.Diary;
import com.ailovedaily.entity.MenuItem;
import com.ailovedaily.entity.Photo;
import com.ailovedaily.entity.User;
import com.ailovedaily.entity.WishList;
import com.ailovedaily.mapper.CoupleLinkMapper;
import com.ailovedaily.mapper.DailyQuoteMapper;
import com.ailovedaily.mapper.DiaryMapper;
import com.ailovedaily.mapper.MenuItemMapper;
import com.ailovedaily.mapper.PhotoMapper;
import com.ailovedaily.mapper.UserMapper;
import com.ailovedaily.mapper.WishListMapper;
import com.ailovedaily.service.AiQuoteService;
import com.ailovedaily.service.HomeService;
import com.ailovedaily.vo.AiQuoteVO;
import com.ailovedaily.vo.DiaryVO;
import com.ailovedaily.vo.HomeVO;
import com.ailovedaily.vo.LoveInfoVO;
import com.ailovedaily.vo.PhotoVO;
import com.ailovedaily.vo.UserVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 首页服务实现。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class HomeServiceImpl implements HomeService {

    private final UserMapper userMapper;
    private final CoupleLinkMapper coupleLinkMapper;
    private final DailyQuoteMapper dailyQuoteMapper;
    private final DiaryMapper diaryMapper;
    private final PhotoMapper photoMapper;
    private final WishListMapper wishListMapper;
    private final MenuItemMapper menuItemMapper;
    private final AiQuoteService aiQuoteService;
    @Autowired(required = false)
    private RedisTemplate<String, Object> redisTemplate;

    @Override
    public HomeVO getHomeData(Long userId) {
        HomeVO homeVO = new HomeVO();

        User user = userMapper.selectById(userId);
        if (user == null) {
            return homeVO;
        }

        UserVO userVO = new UserVO();
        BeanUtil.copyProperties(user, userVO);
        homeVO.setUser(userVO);

        if (user.getCoupleId() != null) {
            User partner = userMapper.selectPartnerByCoupleId(user.getCoupleId(), userId);
            if (partner != null) {
                UserVO partnerVO = new UserVO();
                BeanUtil.copyProperties(partner, partnerVO);
                homeVO.setPartner(partnerVO);
            }

            CoupleLink couple = coupleLinkMapper.selectById(user.getCoupleId());
            if (couple != null) {
                LoveInfoVO loveInfo = new LoveInfoVO();
                loveInfo.setLoveStartDate(couple.getLoveStartDate());
                loveInfo.setLoveMotto(couple.getLoveMotto());
                loveInfo.setCouplePhoto(couple.getCouplePhoto());

                long days = ChronoUnit.DAYS.between(couple.getLoveStartDate(), LocalDate.now());
                loveInfo.setDaysTogether(days);

                long years = days / 365;
                LocalDate nextAnniversary = couple.getLoveStartDate().plusYears(years + 1);
                long nextDays = ChronoUnit.DAYS.between(LocalDate.now(), nextAnniversary);
                loveInfo.setNextAnniversaryDays(nextDays);

                homeVO.setLoveInfo(loveInfo);
            }

            Map<String, Object> quickStats = new HashMap<>();
            quickStats.put("menuCount", menuItemMapper.selectCount(
                    new LambdaQueryWrapper<MenuItem>().eq(MenuItem::getCoupleId, user.getCoupleId())));
            quickStats.put("diaryCount", diaryMapper.selectCount(
                    new LambdaQueryWrapper<Diary>().eq(Diary::getCoupleId, user.getCoupleId())));
            quickStats.put("photoCount", photoMapper.selectCount(
                    new LambdaQueryWrapper<Photo>().eq(Photo::getCoupleId, user.getCoupleId())));
            quickStats.put("wishCount", wishListMapper.selectCount(
                    new LambdaQueryWrapper<WishList>().eq(WishList::getCoupleId, user.getCoupleId())));
            quickStats.put("wishCompletedCount", wishListMapper.selectCount(
                    new LambdaQueryWrapper<WishList>()
                            .eq(WishList::getCoupleId, user.getCoupleId())
                            .eq(WishList::getStatus, 2)));
            homeVO.setQuickStats(quickStats);

            List<Diary> diaries = diaryMapper.selectTimelineByCoupleId(user.getCoupleId(), 3);
            List<DiaryVO> diaryVOList = diaries.stream().map(diary -> {
                DiaryVO diaryVO = new DiaryVO();
                BeanUtil.copyProperties(diary, diaryVO);
                diaryVO.setIsMine(diary.getUserId().equals(userId));

                User author = userMapper.selectById(diary.getUserId());
                if (author != null) {
                    diaryVO.setUserNickname(author.getNickname());
                    diaryVO.setUserAvatar(author.getAvatarUrl());
                }

                // 设置心情文字
                if (diary.getMood() != null) {
                    String[] moodTexts = {"", "开心", "感动", "平静", "难过", "生气"};
                    if (diary.getMood() >= 1 && diary.getMood() <= 5) {
                        diaryVO.setMoodText(moodTexts[diary.getMood()]);
                    }
                }

                if (diary.getImages() != null) {
                    diaryVO.setImages(Arrays.asList(
                            diary.getImages().replace("[", "").replace("]", "").replace("\"", "").split(",")));
                }

                return diaryVO;
            }).collect(Collectors.toList());
            homeVO.setRecentDiaries(diaryVOList);

            List<Photo> photos = photoMapper.selectByCoupleId(user.getCoupleId(), 6);
            List<PhotoVO> photoVOList = photos.stream().map(photo -> {
                PhotoVO photoVO = new PhotoVO();
                BeanUtil.copyProperties(photo, photoVO);

                User uploader = userMapper.selectById(photo.getUserId());
                if (uploader != null) {
                    photoVO.setUserNickname(uploader.getNickname());
                }

                return photoVO;
            }).collect(Collectors.toList());
            homeVO.setRecentPhotos(photoVOList);
        }

        // 优先使用 AI 个性化情话
        if (user.getCoupleId() != null) {
            try {
                AiQuoteVO aiQuote = aiQuoteService.generateQuote(user.getCoupleId(), false);
                if (aiQuote != null && StrUtil.isNotBlank(aiQuote.getContent())) {
                    homeVO.setDailyQuote(aiQuote.getContent());
                    homeVO.setAiQuote(aiQuote);
                } else {
                    homeVO.setDailyQuote(getDailyQuote());
                }
            } catch (Exception e) {
                log.warn("获取 AI 情话失败，降级为随机情话", e);
                homeVO.setDailyQuote(getDailyQuote());
            }
        } else {
            homeVO.setDailyQuote(getDailyQuote());
        }

        return homeVO;
    }

    @Override
    public String getDailyQuote() {
        String cacheKey = "daily:quote:" + LocalDate.now();

        try {
            String cachedQuote = (String) redisTemplate.opsForValue().get(cacheKey);
            if (cachedQuote != null) {
                return cachedQuote;
            }
        } catch (RedisConnectionFailureException exception) {
            log.warn("Redis 不可用，跳过每日情话缓存读取: {}", exception.getMessage());
        } catch (Exception exception) {
            log.warn("读取每日情话缓存失败", exception);
        }

        DailyQuote quote = dailyQuoteMapper.selectRandomByCategory(1);
        if (quote != null) {
            dailyQuoteMapper.incrementUseCount(quote.getId());

            try {
                redisTemplate.opsForValue().set(cacheKey, quote.getContent(), 1, TimeUnit.DAYS);
            } catch (RedisConnectionFailureException exception) {
                log.warn("Redis 不可用，跳过每日情话缓存写入: {}", exception.getMessage());
            } catch (Exception exception) {
                log.warn("写入每日情话缓存失败", exception);
            }

            return quote.getContent();
        }

        return "遇见你，是我这辈子最美的意外。";
    }

    @Override
    public AiQuoteVO getAiDailyQuote(Long userId, boolean force) {
        User user = userMapper.selectById(userId);
        if (user == null || user.getCoupleId() == null) {
            AiQuoteVO vo = new AiQuoteVO();
            vo.setContent(getDailyQuote());
            vo.setAiGenerated(false);
            return vo;
        }
        return aiQuoteService.generateQuote(user.getCoupleId(), force);
    }
}
