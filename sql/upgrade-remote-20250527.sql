-- =====================================================
-- Remote DB Upgrade Script - 2025-05-27
-- Target: ai_love_daily @ 123.60.31.79
-- MySQL Version: 8.0.26
-- =====================================================

USE ai_love_daily;

-- =====================================================
-- 1. daily_quote - add missing columns (from upgrade-ai-quote.sql)
-- =====================================================
ALTER TABLE daily_quote
    ADD COLUMN source TINYINT DEFAULT 0 COMMENT 'source: 0-manual 1-AI' AFTER use_count,
    ADD COLUMN couple_id BIGINT COMMENT 'couple ID for AI personalized' AFTER source,
    ADD COLUMN quote_date DATE COMMENT 'effective date for AI quote cache' AFTER couple_id;

CREATE INDEX idx_couple_date ON daily_quote(couple_id, quote_date);
CREATE INDEX idx_source ON daily_quote(source);

UPDATE daily_quote SET source = 0 WHERE source IS NULL;

-- =====================================================
-- 2. period_daily_log - create new table (from init.sql)
-- =====================================================
CREATE TABLE period_daily_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT 'record ID',
    user_id BIGINT NOT NULL COMMENT 'user ID',
    couple_id BIGINT NOT NULL COMMENT 'couple ID',
    log_date DATE NOT NULL COMMENT 'log date',
    is_period TINYINT DEFAULT 0 COMMENT 'is period: 0-no 1-yes',
    flow_level TINYINT COMMENT 'flow: 1-low 2-medium 3-heavy',
    pain_level TINYINT COMMENT 'pain: 0-none 1-light 2-medium 3-heavy',
    symptoms VARCHAR(500) COMMENT 'symptoms, comma separated',
    mood VARCHAR(100) COMMENT 'mood',
    notes TEXT COMMENT 'notes',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT 'create time',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'update time',
    UNIQUE KEY uk_user_date (user_id, log_date),
    INDEX idx_user_id (user_id),
    INDEX idx_log_date (log_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='period daily log';

-- =====================================================
-- 3. chat_message - create new table (from upgrade-ai-chat.sql)
-- =====================================================
CREATE TABLE chat_message (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT 'Message ID',
    couple_id BIGINT NOT NULL COMMENT 'Couple ID',
    user_id BIGINT NOT NULL COMMENT 'Sender user ID, 0 means assistant',
    role VARCHAR(20) NOT NULL COMMENT 'user/assistant',
    content TEXT NOT NULL COMMENT 'Message content',
    image_url VARCHAR(500) COMMENT 'Attached image URL',
    message_type TINYINT DEFAULT 0 COMMENT '0=text, 1=image',
    conversation_id VARCHAR(64) COMMENT 'Conversation ID',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT 'Create time',
    INDEX idx_couple_conversation_time (couple_id, conversation_id, create_time),
    INDEX idx_couple_time (couple_id, create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI chat messages';

-- =====================================================
-- 4. wardrobe_category - create new table (from upgrade-wardrobe.sql)
-- =====================================================
CREATE TABLE wardrobe_category (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    code VARCHAR(32) NOT NULL UNIQUE,
    name VARCHAR(32) NOT NULL,
    sort_order INT DEFAULT 0,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='wardrobe category dict';

INSERT INTO wardrobe_category (code, name, sort_order) VALUES
  ('top','上衣',1),('bottom','下装',2),('coat','外套',3),
  ('shoes','鞋子',4),('bag','包配饰',5),('inner','内搭',6),('home','家居',7);

-- =====================================================
-- 5. wardrobe_item - create new table (from upgrade-wardrobe.sql)
-- =====================================================
CREATE TABLE wardrobe_item (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    couple_id BIGINT,
    image_url VARCHAR(500) NOT NULL,
    thumb_url VARCHAR(500),
    category_code VARCHAR(32) NOT NULL DEFAULT 'top',
    sub_type VARCHAR(64),
    color VARCHAR(64),
    style VARCHAR(64),
    season VARCHAR(255),
    occasion VARCHAR(255),
    ai_tags VARCHAR(500),
    ai_recognized TINYINT DEFAULT 0,
    favorite TINYINT DEFAULT 0,
    wear_count INT DEFAULT 0,
    last_wear_at TIMESTAMP,
    deleted TINYINT DEFAULT 0,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_wardrobe_user_cat (user_id, category_code, deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='wardrobe item';

-- =====================================================
-- 6. outfit - create new table (from upgrade-wardrobe.sql)
-- =====================================================
CREATE TABLE outfit (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    couple_id BIGINT,
    title VARCHAR(100),
    occasion VARCHAR(64),
    prompt VARCHAR(500),
    ai_generated_image_url VARCHAR(500),
    item_ids VARCHAR(500),
    reason VARCHAR(1000),
    deleted TINYINT DEFAULT 0,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_outfit_user (user_id, create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='outfit plan';

-- =====================================================
-- 7. outfit_item - create new table (from upgrade-wardrobe.sql)
-- =====================================================
CREATE TABLE outfit_item (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    outfit_id BIGINT NOT NULL,
    wardrobe_item_id BIGINT NOT NULL,
    slot VARCHAR(32),
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_outfit_item_outfit (outfit_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='outfit item relation';
