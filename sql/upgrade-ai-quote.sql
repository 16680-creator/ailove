-- =====================================================
-- AI 每日情话功能升级脚本
-- 扩展 daily_quote 表支持 AI 生成记录
-- =====================================================

-- MySQL 环境执行
USE ai_love_daily;

-- 1. 扩展 daily_quote 表
ALTER TABLE daily_quote
    ADD COLUMN source TINYINT DEFAULT 0 COMMENT '来源: 0-人工录入 1-AI生成' AFTER use_count,
    ADD COLUMN couple_id BIGINT COMMENT '关联情侣ID(AI个性化生成时)' AFTER source,
    ADD COLUMN quote_date DATE COMMENT '生效日期(AI情话按天缓存)' AFTER couple_id;

-- 2. 添加索引
CREATE INDEX idx_couple_date ON daily_quote(couple_id, quote_date);
CREATE INDEX idx_source ON daily_quote(source);

-- 3. 更新现有数据为人工录入
UPDATE daily_quote SET source = 0 WHERE source IS NULL;
