package com.ailovedaily.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Redis 安全操作工具类
 * <p>
 * 当 Redis 不可用时，自动跳过所有操作并打印简洁日志，
 * 避免每次请求都等待超时和打印完整堆栈。
 * 内置熔断机制：连续失败 N 次后进入"断路"状态，
 * 定期尝试重连以自动恢复。
 */
@Slf4j
@Component
public class RedisHelper {

    @Autowired(required = false)
    private RedisTemplate<String, Object> redisTemplate;

    /** 连续失败次数达到此值后进入断路状态 */
    private static final int CIRCUIT_THRESHOLD = 3;

    /** 断路状态下，每隔这么久尝试一次重连（毫秒） */
    private static final long RETRY_INTERVAL_MS = 30_000;

    /** 连续失败计数 */
    private final AtomicLong failCount = new AtomicLong(0);

    /** 是否处于断路状态 */
    private final AtomicBoolean circuitOpen = new AtomicBoolean(false);

    /** 上一次尝试重连的时间 */
    private final AtomicLong lastRetryTime = new AtomicLong(0);

    // ==================== 读取 ====================

    public String getString(String key) {
        if (!isAvailable()) {
            return null;
        }
        try {
            Object val = redisTemplate.opsForValue().get(key);
            return val != null ? val.toString() : null;
        } catch (Exception e) {
            onFailure(e);
            return null;
        }
    }

    public Object get(String key) {
        if (!isAvailable()) {
            return null;
        }
        try {
            return redisTemplate.opsForValue().get(key);
        } catch (Exception e) {
            onFailure(e);
            return null;
        }
    }

    // ==================== 写入 ====================

    public void set(String key, Object value, long timeout, TimeUnit unit) {
        if (!isAvailable()) {
            return;
        }
        try {
            redisTemplate.opsForValue().set(key, value, timeout, unit);
            onSuccess();
        } catch (Exception e) {
            onFailure(e);
        }
    }

    // ==================== 删除 ====================

    public Boolean delete(String key) {
        if (!isAvailable()) {
            return false;
        }
        try {
            Boolean result = redisTemplate.delete(key);
            onSuccess();
            return result;
        } catch (Exception e) {
            onFailure(e);
            return false;
        }
    }

    // ==================== 递增 ====================

    public Long increment(String key) {
        if (!isAvailable()) {
            return null;
        }
        try {
            Long result = redisTemplate.opsForValue().increment(key);
            onSuccess();
            return result;
        } catch (Exception e) {
            onFailure(e);
            return null;
        }
    }

    // ==================== 设置过期时间 ====================

    public Boolean expire(String key, long timeout, TimeUnit unit) {
        if (!isAvailable()) {
            return false;
        }
        try {
            Boolean result = redisTemplate.expire(key, timeout, unit);
            onSuccess();
            return result;
        } catch (Exception e) {
            onFailure(e);
            return false;
        }
    }

    // ==================== 模板方法：通用安全执行 ====================

    /**
     * 安全执行 Redis 操作，失败时返回 null 且不影响业务。
     * 适用于较复杂的自定义操作。
     */
    public <T> T executeSafely(RedisOperation<T> operation) {
        if (!isAvailable()) {
            return null;
        }
        try {
            T result = operation.execute();
            onSuccess();
            return result;
        } catch (Exception e) {
            onFailure(e);
            return null;
        }
    }

    @FunctionalInterface
    public interface RedisOperation<T> {
        T execute();
    }

    // ==================== 熔断机制 ====================

    /**
     * 判断 Redis 当前是否可用。
     * 断路状态时每隔 RETRY_INTERVAL_MS 尝试一次重连。
     */
    private boolean isAvailable() {
        if (redisTemplate == null) {
            return false;
        }
        if (!circuitOpen.get()) {
            return true;
        }
        // 断路状态下，检查是否到了重试时间
        long now = System.currentTimeMillis();
        if (now - lastRetryTime.get() >= RETRY_INTERVAL_MS) {
            lastRetryTime.set(now);
            log.info("Redis 断路状态中，尝试重连...");
            return true; // 允许一次调用尝试重连
        }
        return false;
    }

    private void onSuccess() {
        if (circuitOpen.get() || failCount.get() > 0) {
            log.info("Redis 连接恢复正常");
        }
        failCount.set(0);
        circuitOpen.set(false);
    }

    private void onFailure(Exception e) {
        long count = failCount.incrementAndGet();
        if (count >= CIRCUIT_THRESHOLD && circuitOpen.compareAndSet(false, true)) {
            log.warn("Redis 连续失败 {} 次，进入断路状态（{}s 内不再尝试连接），原因: {}",
                    CIRCUIT_THRESHOLD, RETRY_INTERVAL_MS / 1000, e.getMessage());
        } else if (!circuitOpen.get()) {
            // 非断路状态下，只打印简要信息
            log.warn("Redis 操作失败（第{}次）: {}", count, e.getMessage());
        }
        // 断路状态下不再重复打印
    }
}
