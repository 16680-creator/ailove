package com.ailovedaily.service.impl;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.ailovedaily.dto.MealRecordCreateDTO;
import com.ailovedaily.dto.MealReviewDTO;
import com.ailovedaily.entity.MealRecord;
import com.ailovedaily.entity.MenuItem;
import com.ailovedaily.entity.User;
import com.ailovedaily.mapper.MealRecordMapper;
import com.ailovedaily.mapper.MenuItemMapper;
import com.ailovedaily.mapper.UserMapper;
import com.ailovedaily.service.MealRecordService;
import com.ailovedaily.vo.MealRecordVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 餐食记录服务实现类
 */
@Service
@RequiredArgsConstructor
public class MealRecordServiceImpl implements MealRecordService {

    private final MealRecordMapper mealRecordMapper;
    private final MenuItemMapper menuItemMapper;
    private final UserMapper userMapper;

    @Override
    public void createMealRecord(Long userId, MealRecordCreateDTO dto) {
        User user = userMapper.selectById(userId);
        if (user == null || user.getCoupleId() == null) {
            throw new RuntimeException("用户未绑定情侣关系");
        }

        // 构建菜品快照JSON
        JSONArray dishesArray = new JSONArray();
        for (MealRecordCreateDTO.DishItem dish : dto.getDishes()) {
            MenuItem menuItem = menuItemMapper.selectById(dish.getMenuItemId());
            JSONObject obj = new JSONObject();
            obj.set("menuItemId", dish.getMenuItemId());
            obj.set("name", menuItem != null ? menuItem.getName() : "未知菜品");
            obj.set("imageUrl", menuItem != null ? menuItem.getImageUrl() : "");
            obj.set("count", dish.getCount() != null ? dish.getCount() : 1);
            dishesArray.add(obj);
        }

        MealRecord record = new MealRecord();
        record.setCoupleId(user.getCoupleId());
        record.setUserId(userId);
        record.setMealDate(LocalDate.now());
        record.setDishes(dishesArray.toString());
        mealRecordMapper.insert(record);
    }

    @Override
    public void addReview(Long userId, Long recordId, MealReviewDTO dto) {
        MealRecord record = mealRecordMapper.selectById(recordId);
        if (record == null) {
            throw new RuntimeException("记录不存在");
        }

        User user = userMapper.selectById(userId);
        if (user == null || !user.getCoupleId().equals(record.getCoupleId())) {
            throw new RuntimeException("无权评价此记录");
        }

        if (record.getRating() != null) {
            throw new RuntimeException("该记录已评价");
        }

        if (dto.getRating() == null || dto.getRating() < 1 || dto.getRating() > 5) {
            throw new RuntimeException("评分必须在1-5之间");
        }

        record.setRating(dto.getRating());
        record.setComment(dto.getComment());
        record.setReviewBy(userId);
        record.setReviewTime(LocalDateTime.now());
        mealRecordMapper.updateById(record);
    }

    @Override
    public Map<String, List<MealRecordVO>> getHistory(Long userId, Integer days) {
        User user = userMapper.selectById(userId);
        if (user == null || user.getCoupleId() == null) {
            throw new RuntimeException("用户未绑定情侣关系");
        }

        LocalDate startDate = LocalDate.now().minusDays(days != null ? days : 7);
        List<LocalDate> dates = mealRecordMapper.selectDistinctDates(user.getCoupleId(), startDate);

        // 预加载couple内所有用户昵称
        Map<Long, String> nicknameMap = new HashMap<>();
        List<User> users = userMapper.selectByCoupleId(user.getCoupleId());
        for (User u : users) {
            nicknameMap.put(u.getId(), u.getNickname());
        }

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        Map<String, List<MealRecordVO>> result = new LinkedHashMap<>();

        for (LocalDate date : dates) {
            List<MealRecord> records = mealRecordMapper.selectByCoupleIdAndDate(user.getCoupleId(), date);
            List<MealRecordVO> voList = records.stream()
                    .map(r -> toVO(r, nicknameMap))
                    .collect(Collectors.toList());
            result.put(date.format(fmt), voList);
        }

        return result;
    }

    private MealRecordVO toVO(MealRecord record, Map<Long, String> nicknameMap) {
        MealRecordVO vo = new MealRecordVO();
        vo.setId(record.getId());
        vo.setUserId(record.getUserId());
        vo.setUserNickname(nicknameMap.getOrDefault(record.getUserId(), "未知"));
        vo.setMealDate(record.getMealDate());
        vo.setRating(record.getRating());
        vo.setComment(record.getComment());
        vo.setReviewByName(record.getReviewBy() != null ? nicknameMap.getOrDefault(record.getReviewBy(), "未知") : null);
        vo.setReviewTime(record.getReviewTime());
        vo.setCreateTime(record.getCreateTime());
        vo.setCanReview(record.getRating() == null);

        // 解析dishes JSON
        try {
            JSONArray arr = JSONUtil.parseArray(record.getDishes());
            List<MealRecordVO.DishSnapshot> dishes = new ArrayList<>();
            for (int i = 0; i < arr.size(); i++) {
                JSONObject obj = arr.getJSONObject(i);
                MealRecordVO.DishSnapshot snap = new MealRecordVO.DishSnapshot();
                snap.setMenuItemId(obj.getLong("menuItemId"));
                snap.setName(obj.getStr("name"));
                snap.setImageUrl(obj.getStr("imageUrl"));
                snap.setCount(obj.getInt("count"));
                dishes.add(snap);
            }
            vo.setDishes(dishes);
        } catch (Exception e) {
            vo.setDishes(Collections.emptyList());
        }

        return vo;
    }
}
