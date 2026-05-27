package com.ailovedaily.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.ailovedaily.dto.AccountLoginDTO;
import com.ailovedaily.dto.LoginDTO;
import com.ailovedaily.dto.RegisterDTO;
import com.ailovedaily.dto.UserUpdateDTO;
import com.ailovedaily.entity.CoupleLink;
import com.ailovedaily.entity.User;
import com.ailovedaily.mapper.CoupleLinkMapper;
import com.ailovedaily.mapper.UserMapper;
import com.ailovedaily.service.UserService;
import com.ailovedaily.utils.JwtUtil;
import com.ailovedaily.vo.LoveInfoVO;
import com.ailovedaily.vo.UserVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 用户服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;
    private final CoupleLinkMapper coupleLinkMapper;
    private final JwtUtil jwtUtil;
    @Autowired(required = false)
    private RedisTemplate<String, Object> redisTemplate;
    private final PasswordEncoder passwordEncoder;

    @Value("${wx.appid}")
    private String appid;

    @Value("${wx.secret}")
    private String secret;

    @Override
    public Map<String, Object> devLogin(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            // 不存在则自动创建一个测试用户
            user = new User();
            user.setId(userId);
            user.setOpenid("dev_openid_" + userId);
            user.setNickname("测试用户" + userId);
            user.setStatus(1);
            userMapper.insert(user);
        }

        user.setLastLoginTime(java.time.LocalDateTime.now());
        userMapper.updateById(user);

        return buildLoginResult(user, false);
    }

    @Override
    public Map<String, Object> wxLogin(LoginDTO loginDTO) {
        // 调用微信接口获取openid
        String url = String.format(
                "https://api.weixin.qq.com/sns/jscode2session?appid=%s&secret=%s&js_code=%s&grant_type=authorization_code",
                appid, secret, loginDTO.getCode()
        );

        String response = HttpUtil.get(url);
        JSONObject jsonObject = JSONUtil.parseObj(response);

        if (jsonObject.containsKey("errcode")) {
            log.error("微信登录失败: {}", response);
            throw new RuntimeException("微信登录失败: " + jsonObject.getStr("errmsg"));
        }

        String openid = jsonObject.getStr("openid");
        String sessionKey = jsonObject.getStr("session_key");
        String unionid = jsonObject.getStr("unionid");

        // 查询或创建用户
        User user = userMapper.selectByOpenid(openid);
        boolean isNewUser = false;

        if (user == null) {
            user = new User();
            user.setOpenid(openid);
            user.setUnionid(unionid);
            user.setStatus(1);
            userMapper.insert(user);
            isNewUser = true;
        }

        // 更新最后登录时间
        user.setLastLoginTime(java.time.LocalDateTime.now());
        userMapper.updateById(user);

        // 缓存session_key（用于解密用户信息）
        try {
            redisTemplate.opsForValue().set("session_key:" + user.getId(), sessionKey, 30, TimeUnit.DAYS);
        } catch (RedisConnectionFailureException exception) {
            log.warn("Redis 不可用，跳过 session_key 缓存: {}", exception.getMessage());
        } catch (Exception exception) {
            log.warn("缓存 session_key 失败", exception);
        }

        return buildLoginResult(user, isNewUser);
    }

    @Override
    public Map<String, Object> accountLogin(AccountLoginDTO loginDTO) {
        String loginName = normalizeLoginName(loginDTO.getLoginName());
        User user = userMapper.selectByLoginName(loginName);
        if (user == null) {
            throw new RuntimeException("账号不存在");
        }

        if (user.getStatus() == null || user.getStatus() != 1) {
            throw new RuntimeException("账号已被禁用");
        }

        if (StrUtil.isBlank(user.getPasswordHash()) || !passwordEncoder.matches(loginDTO.getPassword(), user.getPasswordHash())) {
            throw new RuntimeException("账号或密码错误");
        }

        user.setLastLoginTime(java.time.LocalDateTime.now());
        userMapper.updateById(user);

        return buildLoginResult(user, false);
    }

    @Override
    public Map<String, Object> register(RegisterDTO registerDTO) {
        String loginName = normalizeLoginName(registerDTO.getLoginName());
        User existedUser = userMapper.selectByLoginName(loginName);
        if (existedUser != null) {
            throw new RuntimeException("登录账号已存在");
        }

        User user = new User();
        user.setOpenid("web_" + IdUtil.fastSimpleUUID());
        user.setLoginName(loginName);
        user.setPasswordHash(passwordEncoder.encode(registerDTO.getPassword()));
        user.setNickname(registerDTO.getNickname().trim());
        user.setPhone(StrUtil.blankToDefault(StrUtil.trim(registerDTO.getPhone()), null));
        user.setGender(0);
        user.setStatus(1);
        user.setLastLoginTime(java.time.LocalDateTime.now());
        userMapper.insert(user);

        return buildLoginResult(user, true);
    }

    @Override
    public User getById(Long id) {
        return userMapper.selectById(id);
    }

    @Override
    public UserVO getUserInfo(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            return null;
        }

        UserVO userVO = new UserVO();
        BeanUtil.copyProperties(user, userVO);

        // 获取伴侣信息
        if (user.getCoupleId() != null) {
            User partner = userMapper.selectPartnerByCoupleId(user.getCoupleId(), userId);
            if (partner != null) {
                UserVO partnerVO = new UserVO();
                BeanUtil.copyProperties(partner, partnerVO);
                userVO.setPartner(partnerVO);

                // 获取恋爱信息
                CoupleLink couple = coupleLinkMapper.selectById(user.getCoupleId());
                if (couple != null) {
                    LoveInfoVO loveInfo = new LoveInfoVO();
                    loveInfo.setLoveStartDate(couple.getLoveStartDate());
                    loveInfo.setLoveMotto(couple.getLoveMotto());

                    // 计算在一起天数
                    long days = ChronoUnit.DAYS.between(couple.getLoveStartDate(), LocalDate.now());
                    loveInfo.setDaysTogether(days);

                    // 计算下一纪念日天数
                    LocalDate nextAnniversary = couple.getLoveStartDate().plusYears(days / 365 + 1);
                    long nextDays = ChronoUnit.DAYS.between(LocalDate.now(), nextAnniversary);
                    loveInfo.setNextAnniversaryDays(nextDays);

                    userVO.setLoveInfo(loveInfo);
                }
            }
        }

        return userVO;
    }

    @Override
    public void updateUser(Long userId, UserUpdateDTO updateDTO) {
        User user = new User();
        user.setId(userId);
        BeanUtil.copyProperties(updateDTO, user);
        userMapper.updateById(user);
    }

    @Override
    public UserVO getPartnerInfo(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null || user.getCoupleId() == null) {
            return null;
        }

        User partner = userMapper.selectPartnerByCoupleId(user.getCoupleId(), userId);
        if (partner == null) {
            return null;
        }

        UserVO partnerVO = new UserVO();
        BeanUtil.copyProperties(partner, partnerVO);
        return partnerVO;
    }

    private String normalizeLoginName(String loginName) {
        return StrUtil.trim(loginName).toLowerCase();
    }

    private Map<String, Object> buildLoginResult(User user, boolean isNewUser) {
        String token = jwtUtil.generateToken(user.getId(), user.getOpenid());

        Map<String, Object> result = new HashMap<>();
        result.put("token", token);
        result.put("isNewUser", isNewUser);
        result.put("hasCouple", user.getCoupleId() != null);
        return result;
    }
}
