package com.ailovedaily.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.ailovedaily.dto.PeriodDailyLogDTO;
import com.ailovedaily.dto.PeriodRecordDTO;
import com.ailovedaily.entity.PeriodDailyLog;
import com.ailovedaily.entity.PeriodRecord;
import com.ailovedaily.entity.User;
import com.ailovedaily.exception.NotFoundException;
import com.ailovedaily.exception.UnauthorizedException;
import com.ailovedaily.mapper.PeriodDailyLogMapper;
import com.ailovedaily.mapper.PeriodRecordMapper;
import com.ailovedaily.mapper.UserMapper;
import com.ailovedaily.service.PeriodService;
import com.ailovedaily.vo.PeriodDailyLogVO;
import com.ailovedaily.vo.PeriodInfoVO;
import com.ailovedaily.vo.PeriodRecordVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 生理期服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PeriodServiceImpl implements PeriodService {

    private final PeriodRecordMapper periodRecordMapper;
    private final PeriodDailyLogMapper periodDailyLogMapper;
    private final UserMapper userMapper;
    @Autowired(required = false)
    private RedisTemplate<String, Object> redisTemplate;

    private static final String PERIOD_INFO_CACHE_KEY = "period:info:";
    private static final long CACHE_EXPIRE_HOURS = 2;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void recordPeriod(Long userId, PeriodRecordDTO recordDTO) {
        User user = userMapper.selectById(userId);
        if (user == null || user.getCoupleId() == null) {
            throw new IllegalStateException("用户未绑定情侣关系");
        }

        // 验证性别
        if (user.getGender() != null && user.getGender() != 2) {
            throw new IllegalStateException("只有女生可以记录生理期");
        }

        PeriodRecord record = new PeriodRecord();
        BeanUtil.copyProperties(recordDTO, record);
        record.setUserId(userId);
        record.setCoupleId(user.getCoupleId());
        record.setIsPredicted(0);

        // 如果未设置经期天数，自动计算
        if (record.getPeriodDays() == null && record.getEndDate() != null) {
            record.setPeriodDays((int) ChronoUnit.DAYS.between(record.getStartDate(), record.getEndDate()) + 1);
        }

        periodRecordMapper.insert(record);

        // 清除缓存
        clearPeriodCache(userId);

        // 重新生成预测
        generatePredictions(userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updatePeriod(Long id, Long userId, PeriodRecordDTO recordDTO) {
        PeriodRecord record = periodRecordMapper.selectById(id);
        if (record == null) {
            throw NotFoundException.of("生理期记录", id);
        }

        if (!record.getUserId().equals(userId)) {
            throw new UnauthorizedException("无权修改他人记录");
        }

        BeanUtil.copyProperties(recordDTO, record);
        record.setId(id);

        // 如果未设置经期天数，自动计算
        if (record.getEndDate() != null) {
            record.setPeriodDays((int) ChronoUnit.DAYS.between(record.getStartDate(), record.getEndDate()) + 1);
        }

        periodRecordMapper.updateById(record);

        // 清除缓存
        clearPeriodCache(userId);

        // 重新生成预测
        generatePredictions(userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deletePeriod(Long id, Long userId) {
        PeriodRecord record = periodRecordMapper.selectById(id);
        if (record == null) {
            throw NotFoundException.of("生理期记录", id);
        }

        if (!record.getUserId().equals(userId)) {
            throw new UnauthorizedException("无权删除他人记录");
        }

        periodRecordMapper.deleteById(id);

        // 清除缓存
        clearPeriodCache(userId);

        // 重新生成预测
        generatePredictions(userId);
    }

    @Override
    public PeriodInfoVO getPeriodInfo(Long userId) {
        // 尝试从缓存获取
        String cacheKey = PERIOD_INFO_CACHE_KEY + userId;
        try {
            Object cached = redisTemplate.opsForValue().get(cacheKey);
            if (cached instanceof PeriodInfoVO) {
                return (PeriodInfoVO) cached;
            }
        } catch (Exception e) {
            log.warn("Redis缓存读取失败，跳过缓存: {}", e.getMessage());
        }

        User user = userMapper.selectById(userId);
        if (user == null) {
            return null;
        }

        PeriodInfoVO info = new PeriodInfoVO();
        info.setUserId(userId);
        info.setUserNickname(user.getNickname());

        // 获取所有实际记录
        List<PeriodRecord> actualRecords = periodRecordMapper.selectActualRecordsByUserId(userId);

        // 计算平均值
        if (!actualRecords.isEmpty()) {
            int avgCycle = calculateAverageCycle(actualRecords);
            int avgPeriod = calculateAveragePeriod(actualRecords);

            info.setAvgCycleDays(avgCycle);
            info.setAvgPeriodDays(avgPeriod);

            // 预测下次日期
            LocalDate lastStart = actualRecords.get(0).getStartDate();
            LocalDate nextPredicted = lastStart.plusDays(avgCycle);
            info.setNextPredictedDate(nextPredicted);

            long daysUntil = ChronoUnit.DAYS.between(LocalDate.now(), nextPredicted);
            info.setDaysUntilNext(daysUntil);

            // 判断当前状态
            int status = determineCurrentStatus(lastStart, avgPeriod, avgCycle);
            info.setCurrentStatus(status);
            switch (status) {
                case 2: info.setCurrentStatusText("经期中"); break;
                case 1: info.setCurrentStatusText("易孕期"); break;
                default: info.setCurrentStatusText("安全期"); break;
            }

            // 是否需要提醒（2天内）
            info.setNeedRemind(daysUntil >= 0 && daysUntil <= 2);
        } else {
            info.setAvgCycleDays(28);
            info.setAvgPeriodDays(5);
        }

        // 获取最近记录
        info.setRecentRecords(getRecentRecords(userId, 6));

        // 存入缓存
        try {
            redisTemplate.opsForValue().set(cacheKey, info, CACHE_EXPIRE_HOURS, TimeUnit.HOURS);
        } catch (Exception e) {
            log.warn("Redis缓存写入失败，跳过缓存: {}", e.getMessage());
        }

        return info;
    }

    @Override
    public List<PeriodRecordVO> getRecentRecords(Long userId, Integer limit) {
        List<PeriodRecord> records = periodRecordMapper.selectActualRecordsByUserId(userId);
        return records.stream().limit(limit).map(this::convertToVO).collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void generatePredictions(Long userId) {
        // 删除旧的预测记录
        periodRecordMapper.deletePredictedByUserId(userId);

        // 获取最近实际记录
        PeriodRecord lastRecord = periodRecordMapper.selectLatestByUserId(userId);
        if (lastRecord == null) {
            return;
        }

        // 计算平均周期
        List<PeriodRecord> actualRecords = periodRecordMapper.selectActualRecordsByUserId(userId);
        int avgCycle = calculateAverageCycle(actualRecords);

        // 生成未来3个月的预测
        User user = userMapper.selectById(userId);
        LocalDate lastStart = lastRecord.getStartDate();
        int periodDays = lastRecord.getPeriodDays() != null ? lastRecord.getPeriodDays() : 5;

        for (int i = 1; i <= 3; i++) {
            LocalDate predictedStart = lastStart.plusDays((long) avgCycle * i);

            PeriodRecord prediction = new PeriodRecord();
            prediction.setUserId(userId);
            prediction.setCoupleId(user.getCoupleId());
            prediction.setStartDate(predictedStart);
            prediction.setEndDate(predictedStart.plusDays(periodDays - 1));
            prediction.setCycleDays(avgCycle);
            prediction.setPeriodDays(periodDays);
            prediction.setIsPredicted(1);

            periodRecordMapper.insert(prediction);
        }
    }

    @Override
    public void checkAndSendReminders() {
        // 查询所有情侣
        List<User> femaleUsers = userMapper.selectList(
                new LambdaQueryWrapper<User>()
                        .eq(User::getGender, 2)
                        .isNotNull(User::getCoupleId));

        for (User female : femaleUsers) {
            try {
                PeriodInfoVO info = getPeriodInfo(female.getId());

                if (info != null && info.getNeedRemind() != null && info.getNeedRemind()) {
                    // 获取伴侣（男生）
                    User partner = userMapper.selectPartnerByCoupleId(female.getCoupleId(), female.getId());

                    if (partner != null) {
                        // TODO: 发送微信服务通知
                        log.info("需要发送生理期提醒给男生: {}, 女生: {}, 预计还有 {} 天",
                                partner.getNickname(), female.getNickname(), info.getDaysUntilNext());
                    }
                }
            } catch (Exception e) {
                log.error("处理用户 {} 的生理期提醒失败", female.getId(), e);
            }
        }
    }

    /**
     * 计算平均周期天数
     */
    private int calculateAverageCycle(List<PeriodRecord> actualRecords) {
        if (actualRecords.size() < 2) {
            return 28;
        }

        int totalCycle = 0;
        int cycleCount = 0;

        for (int i = 0; i < actualRecords.size() - 1; i++) {
            PeriodRecord current = actualRecords.get(i);
            PeriodRecord next = actualRecords.get(i + 1);

            int cycleDays = (int) ChronoUnit.DAYS.between(next.getStartDate(), current.getStartDate());
            if (cycleDays > 20 && cycleDays < 40) { // 排除异常数据
                totalCycle += cycleDays;
                cycleCount++;
            }
        }

        return cycleCount > 0 ? totalCycle / cycleCount : 28;
    }

    /**
     * 计算平均经期天数
     */
    private int calculateAveragePeriod(List<PeriodRecord> actualRecords) {
        if (actualRecords.isEmpty()) {
            return 5;
        }

        int totalPeriod = 0;
        int count = 0;

        for (PeriodRecord record : actualRecords) {
            if (record.getPeriodDays() != null && record.getPeriodDays() > 0) {
                totalPeriod += record.getPeriodDays();
                count++;
            }
        }

        return count > 0 ? totalPeriod / count : 5;
    }

    /**
     * 判断当前状态
     */
    private int determineCurrentStatus(LocalDate lastStart, int avgPeriod, int avgCycle) {
        LocalDate today = LocalDate.now();

        // 检查是否在上次实际经期中
        if (!today.isBefore(lastStart) && today.isBefore(lastStart.plusDays(avgPeriod))) {
            return 2; // 经期
        }

        // 检查是否在预测经期中
        LocalDate nextPredicted = lastStart.plusDays(avgCycle);
        if (!today.isBefore(nextPredicted) && today.isBefore(nextPredicted.plusDays(avgPeriod))) {
            return 2; // 经期
        }

        return 0; // 安全期
    }

    /**
     * 清除生理期缓存
     */
    private void clearPeriodCache(Long userId) {
        String cacheKey = PERIOD_INFO_CACHE_KEY + userId;
        try {
            redisTemplate.delete(cacheKey);
        } catch (Exception e) {
            log.warn("Redis缓存清除失败，跳过: {}", e.getMessage());
        }
    }

    private PeriodRecordVO convertToVO(PeriodRecord record) {
        PeriodRecordVO vo = new PeriodRecordVO();
        BeanUtil.copyProperties(record, vo);
        vo.setIsPredicted(record.getIsPredicted() != null && record.getIsPredicted() == 1);

        // 设置流量文本
        if (record.getFlowLevel() != null) {
            switch (record.getFlowLevel()) {
                case 1: vo.setFlowLevelText("少"); break;
                case 2: vo.setFlowLevelText("中"); break;
                case 3: vo.setFlowLevelText("多"); break;
                default: vo.setFlowLevelText(""); break;
            }
        }

        // 设置疼痛文本
        if (record.getPainLevel() != null) {
            switch (record.getPainLevel()) {
                case 0: vo.setPainLevelText("无"); break;
                case 1: vo.setPainLevelText("轻"); break;
                case 2: vo.setPainLevelText("中"); break;
                case 3: vo.setPainLevelText("重"); break;
                default: vo.setPainLevelText(""); break;
            }
        }

        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveDailyLog(Long userId, PeriodDailyLogDTO dto) {
        User user = userMapper.selectById(userId);
        if (user == null || user.getCoupleId() == null) {
            throw new IllegalStateException("用户未绑定情侣关系");
        }

        // 按 (userId, logDate) 查找已有记录
        LambdaQueryWrapper<PeriodDailyLog> wrapper = new LambdaQueryWrapper<PeriodDailyLog>()
                .eq(PeriodDailyLog::getUserId, userId)
                .eq(PeriodDailyLog::getLogDate, dto.getLogDate());
        PeriodDailyLog existing = periodDailyLogMapper.selectOne(wrapper);

        if (existing != null) {
            // 更新
            existing.setIsPeriod(dto.getIsPeriod() != null ? dto.getIsPeriod() : 0);
            existing.setFlowLevel(dto.getFlowLevel());
            existing.setPainLevel(dto.getPainLevel());
            existing.setSymptoms(dto.getSymptoms());
            existing.setMood(dto.getMood());
            existing.setNotes(dto.getNotes());
            periodDailyLogMapper.updateById(existing);
        } else {
            // 新增
            PeriodDailyLog log = new PeriodDailyLog();
            log.setUserId(userId);
            log.setCoupleId(user.getCoupleId());
            log.setLogDate(dto.getLogDate());
            log.setIsPeriod(dto.getIsPeriod() != null ? dto.getIsPeriod() : 0);
            log.setFlowLevel(dto.getFlowLevel());
            log.setPainLevel(dto.getPainLevel());
            log.setSymptoms(dto.getSymptoms());
            log.setMood(dto.getMood());
            log.setNotes(dto.getNotes());
            periodDailyLogMapper.insert(log);
        }

        // 如果标记为非经期，检查前一天是否为经期，若是则结束周期
        if (dto.getIsPeriod() != null && dto.getIsPeriod() == 0) {
            LocalDate prevDate = dto.getLogDate().minusDays(1);
            LambdaQueryWrapper<PeriodDailyLog> prevWrapper = new LambdaQueryWrapper<PeriodDailyLog>()
                    .eq(PeriodDailyLog::getUserId, userId)
                    .eq(PeriodDailyLog::getLogDate, prevDate)
                    .eq(PeriodDailyLog::getIsPeriod, 1);
            PeriodDailyLog prevLog = periodDailyLogMapper.selectOne(prevWrapper);
            if (prevLog != null) {
                finalizeCycle(userId, user.getCoupleId());
            }
        }

        clearPeriodCache(userId);
    }

    /**
     * 从连续 isPeriod=1 的日志中生成一条 period_record
     */
    private void finalizeCycle(Long userId, Long coupleId) {
        // 查询最近的经期日志，按日期倒序
        LambdaQueryWrapper<PeriodDailyLog> wrapper = new LambdaQueryWrapper<PeriodDailyLog>()
                .eq(PeriodDailyLog::getUserId, userId)
                .eq(PeriodDailyLog::getIsPeriod, 1)
                .orderByDesc(PeriodDailyLog::getLogDate);
        List<PeriodDailyLog> periodDays = periodDailyLogMapper.selectList(wrapper);

        if (periodDays.isEmpty()) {
            return;
        }

        // 找连续日期段：从最近一天往前找
        LocalDate endDate = periodDays.get(0).getLogDate();
        LocalDate startDate = endDate;
        for (int i = 1; i < periodDays.size(); i++) {
            LocalDate current = periodDays.get(i).getLogDate();
            if (current.equals(startDate.minusDays(1))) {
                startDate = current;
            } else {
                break;
            }
        }

        int periodDaysCount = (int) (ChronoUnit.DAYS.between(startDate, endDate) + 1);

        // 计算周期天数：与上一条实际记录的间隔
        PeriodRecord lastRecord = periodRecordMapper.selectLatestByUserId(userId);
        int cycleDays = 28;
        if (lastRecord != null) {
            int days = (int) ChronoUnit.DAYS.between(lastRecord.getStartDate(), startDate);
            if (days > 20 && days < 40) {
                cycleDays = days;
            }
        }

        PeriodRecord record = new PeriodRecord();
        record.setUserId(userId);
        record.setCoupleId(coupleId);
        record.setStartDate(startDate);
        record.setEndDate(endDate);
        record.setCycleDays(cycleDays);
        record.setPeriodDays(periodDaysCount);
        record.setIsPredicted(0);
        periodRecordMapper.insert(record);

        // 重新生成预测
        generatePredictions(userId);
    }

    @Override
    public List<PeriodDailyLogVO> getMonthlyLogs(Long userId, int year, int month) {
        List<PeriodDailyLog> logs = periodDailyLogMapper.selectByMonth(userId, year, month);
        return logs.stream().map(this::convertDailyLogToVO).collect(Collectors.toList());
    }

    @Override
    public PeriodDailyLogVO getDailyLog(Long userId, LocalDate date) {
        LambdaQueryWrapper<PeriodDailyLog> wrapper = new LambdaQueryWrapper<PeriodDailyLog>()
                .eq(PeriodDailyLog::getUserId, userId)
                .eq(PeriodDailyLog::getLogDate, date);
        PeriodDailyLog log = periodDailyLogMapper.selectOne(wrapper);
        return log != null ? convertDailyLogToVO(log) : null;
    }

    private PeriodDailyLogVO convertDailyLogToVO(PeriodDailyLog log) {
        PeriodDailyLogVO vo = new PeriodDailyLogVO();
        BeanUtil.copyProperties(log, vo);

        if (log.getFlowLevel() != null) {
            switch (log.getFlowLevel()) {
                case 1: vo.setFlowLevelText("少"); break;
                case 2: vo.setFlowLevelText("中"); break;
                case 3: vo.setFlowLevelText("多"); break;
                default: vo.setFlowLevelText(""); break;
            }
        }

        if (log.getPainLevel() != null) {
            switch (log.getPainLevel()) {
                case 0: vo.setPainLevelText("无"); break;
                case 1: vo.setPainLevelText("轻"); break;
                case 2: vo.setPainLevelText("中"); break;
                case 3: vo.setPainLevelText("重"); break;
                default: vo.setPainLevelText(""); break;
            }
        }

        return vo;
    }
}
