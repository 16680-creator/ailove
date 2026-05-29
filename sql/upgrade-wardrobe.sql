-- =====================================================
-- 智能衣柜功能 升级脚本
-- 数据库: MySQL 8.0
-- =====================================================

USE ai_love_daily;

-- 1. 衣物分类字典表
CREATE TABLE IF NOT EXISTS wardrobe_category (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    code VARCHAR(32) NOT NULL UNIQUE,
    name VARCHAR(32) NOT NULL,
    sort_order INT DEFAULT 0,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

INSERT INTO wardrobe_category (code, name, sort_order) VALUES
  ('top','上衣',1),('bottom','下装',2),('coat','外套',3),
  ('shoes','鞋子',4),('bag','包配饰',5),('inner','内搭',6),('home','家居',7);

-- 2. 衣物表
CREATE TABLE IF NOT EXISTS wardrobe_item (
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
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_wardrobe_user_cat ON wardrobe_item(user_id, category_code, deleted);

-- 3. 穿搭方案表
CREATE TABLE IF NOT EXISTS outfit (
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
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_outfit_user ON outfit(user_id, create_time);

-- 4. 穿搭方案衣物关联表
CREATE TABLE IF NOT EXISTS outfit_item (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    outfit_id BIGINT NOT NULL,
    wardrobe_item_id BIGINT NOT NULL,
    slot VARCHAR(32),
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_outfit_item_outfit ON outfit_item(outfit_id);
