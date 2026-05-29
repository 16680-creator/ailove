-- =====================================================
-- 爱在朝夕 - 情侣生活记录小程序 数据库初始化脚本
-- 数据库: MySQL 8.0
-- =====================================================

-- 创建数据库
CREATE DATABASE IF NOT EXISTS ai_love_daily DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE ai_love_daily;

-- =====================================================
-- 1. 用户表 (sys_user)
-- =====================================================
CREATE TABLE sys_user (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '用户ID',
    openid VARCHAR(100) UNIQUE NOT NULL COMMENT '微信OpenID',
    unionid VARCHAR(100) COMMENT '微信UnionID',
    login_name VARCHAR(50) UNIQUE COMMENT 'Web登录账号',
    password_hash VARCHAR(255) COMMENT 'Web登录密码摘要',
    nickname VARCHAR(50) COMMENT '昵称',
    avatar_url VARCHAR(500) COMMENT '头像URL',
    gender TINYINT DEFAULT 0 COMMENT '性别: 0-未知 1-男 2-女',
    phone VARCHAR(20) COMMENT '手机号',
    birthday DATE COMMENT '生日',
    couple_id BIGINT COMMENT '情侣关系ID',
    role TINYINT COMMENT '角色: 1-发起方 2-接受方',
    status TINYINT DEFAULT 1 COMMENT '状态: 0-禁用 1-正常',
    last_login_time DATETIME COMMENT '最后登录时间',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_openid (openid),
    INDEX idx_login_name (login_name),
    INDEX idx_couple_id (couple_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- =====================================================
-- 2. 情侣关系表 (couple_link)
-- =====================================================
CREATE TABLE couple_link (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '关系ID',
    user1_id BIGINT NOT NULL COMMENT '用户1ID',
    user2_id BIGINT COMMENT '用户2ID',
    love_start_date DATE NOT NULL COMMENT '恋爱开始日期',
    love_motto VARCHAR(255) COMMENT '爱情宣言',
    couple_photo VARCHAR(500) COMMENT '合照URL',
    invite_code VARCHAR(20) UNIQUE COMMENT '邀请码',
    status TINYINT DEFAULT 0 COMMENT '状态: 0-待绑定 1-已绑定 2-已解绑',
    bind_time DATETIME COMMENT '绑定时间',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_user1 (user1_id),
    INDEX idx_user2 (user2_id),
    INDEX idx_invite_code (invite_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='情侣关系表';

-- =====================================================
-- 3. 每日一言表 (daily_quote)
-- =====================================================
CREATE TABLE daily_quote (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT 'ID',
    content VARCHAR(500) NOT NULL COMMENT '内容',
    author VARCHAR(50) COMMENT '作者',
    category TINYINT DEFAULT 1 COMMENT '分类: 1-情话 2-励志 3-幽默',
    use_count INT DEFAULT 0 COMMENT '使用次数',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_category (category)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='每日一言表';

-- =====================================================
-- 4. 菜品表 (menu_item)
-- =====================================================
CREATE TABLE menu_item (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '菜品ID',
    couple_id BIGINT NOT NULL COMMENT '情侣关系ID',
    name VARCHAR(100) NOT NULL COMMENT '菜品名称',
    image_url VARCHAR(500) COMMENT '图片URL',
    category TINYINT DEFAULT 1 COMMENT '分类: 1-家常菜 2-西餐 3-小吃 4-甜品 5-饮品',
    tags VARCHAR(200) COMMENT '标签,逗号分隔',
    difficulty TINYINT DEFAULT 3 COMMENT '难度: 1-5星',
    cook_time INT COMMENT '烹饪时间(分钟)',
    description TEXT COMMENT '描述',
    recipe TEXT COMMENT 'AI生成的做法',
    create_by BIGINT COMMENT '创建人ID',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_couple_id (couple_id),
    INDEX idx_category (category)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='菜品表';

-- =====================================================
-- 5. 日记表 (diary)
-- =====================================================
CREATE TABLE diary (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '日记ID',
    couple_id BIGINT NOT NULL COMMENT '情侣关系ID',
    user_id BIGINT NOT NULL COMMENT '作者ID',
    title VARCHAR(200) COMMENT '标题',
    content TEXT COMMENT '内容',
    mood TINYINT COMMENT '心情: 1-开心 2-感动 3-平静 4-难过 5-生气',
    weather VARCHAR(20) COMMENT '天气',
    location VARCHAR(100) COMMENT '地点',
    images JSON COMMENT '图片数组',
    is_favorite TINYINT DEFAULT 0 COMMENT '是否收藏: 0-否 1-是',
    view_count INT DEFAULT 0 COMMENT '浏览次数',
    diary_date DATE COMMENT '日记日期',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_couple_id (couple_id),
    INDEX idx_user_id (user_id),
    INDEX idx_diary_date (diary_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='日记表';

-- =====================================================
-- 6. 相册表 (photo_album)
-- =====================================================
CREATE TABLE photo_album (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '相册ID',
    couple_id BIGINT NOT NULL COMMENT '情侣关系ID',
    name VARCHAR(100) NOT NULL COMMENT '相册名称',
    cover_url VARCHAR(500) COMMENT '封面图片',
    description VARCHAR(500) COMMENT '描述',
    photo_count INT DEFAULT 0 COMMENT '照片数量',
    sort_order INT DEFAULT 0 COMMENT '排序',
    create_by BIGINT COMMENT '创建人ID',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_couple_id (couple_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='相册表';

-- =====================================================
-- 7. 照片表 (photo)
-- =====================================================
CREATE TABLE photo (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '照片ID',
    album_id BIGINT NOT NULL COMMENT '相册ID',
    couple_id BIGINT NOT NULL COMMENT '情侣关系ID',
    user_id BIGINT NOT NULL COMMENT '上传人ID',
    url VARCHAR(500) NOT NULL COMMENT '图片URL',
    thumbnail_url VARCHAR(500) COMMENT '缩略图URL',
    description VARCHAR(500) COMMENT '描述',
    location VARCHAR(100) COMMENT '拍摄地点',
    shoot_time DATETIME COMMENT '拍摄时间',
    file_size BIGINT COMMENT '文件大小(字节)',
    width INT COMMENT '图片宽度',
    height INT COMMENT '图片高度',
    is_favorite TINYINT DEFAULT 0 COMMENT '是否收藏',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_album_id (album_id),
    INDEX idx_couple_id (couple_id),
    INDEX idx_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='照片表';

-- =====================================================
-- 8. 心愿清单表 (wish_list)
-- =====================================================
CREATE TABLE wish_list (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '心愿ID',
    couple_id BIGINT NOT NULL COMMENT '情侣关系ID',
    user_id BIGINT NOT NULL COMMENT '创建人ID',
    title VARCHAR(200) NOT NULL COMMENT '心愿标题',
    description TEXT COMMENT '心愿描述',
    image_url VARCHAR(500) COMMENT '心愿图片',
    category TINYINT DEFAULT 1 COMMENT '分类: 1-旅行 2-美食 3-购物 4-体验 5-其他',
    priority TINYINT DEFAULT 3 COMMENT '优先级: 1-低 2-中 3-高 4-紧急',
    status TINYINT DEFAULT 0 COMMENT '状态: 0-待完成 1-进行中 2-已完成 3-已放弃',
    target_date DATE COMMENT '目标完成日期',
    complete_date DATE COMMENT '实际完成日期',
    complete_by BIGINT COMMENT '完成人ID',
    linked_diary_id BIGINT COMMENT '关联日记ID',
    linked_photo_ids JSON COMMENT '关联照片ID数组',
    sort_order INT DEFAULT 0 COMMENT '排序',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_couple_id (couple_id),
    INDEX idx_user_id (user_id),
    INDEX idx_status (status),
    INDEX idx_category (category)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='心愿清单表';

-- =====================================================
-- 9. 生理期记录表 (period_record)
-- =====================================================
CREATE TABLE period_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '记录ID',
    user_id BIGINT NOT NULL COMMENT '用户ID(女生)',
    couple_id BIGINT NOT NULL COMMENT '情侣关系ID',
    start_date DATE NOT NULL COMMENT '开始日期',
    end_date DATE COMMENT '结束日期',
    cycle_days INT DEFAULT 28 COMMENT '周期天数',
    period_days INT DEFAULT 5 COMMENT '经期天数',
    symptoms VARCHAR(500) COMMENT '症状,逗号分隔',
    mood VARCHAR(100) COMMENT '心情',
    flow_level TINYINT COMMENT '流量: 1-少 2-中 3-多',
    pain_level TINYINT COMMENT '疼痛: 0-无 1-轻 2-中 3-重',
    notes TEXT COMMENT '备注',
    is_predicted TINYINT DEFAULT 0 COMMENT '是否预测数据: 0-实际 1-预测',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_user_id (user_id),
    INDEX idx_couple_id (couple_id),
    INDEX idx_start_date (start_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='生理期记录表';

-- =====================================================
-- 10. 每日经期打卡表 (period_daily_log)
-- =====================================================
CREATE TABLE period_daily_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '记录ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    couple_id BIGINT NOT NULL COMMENT '情侣关系ID',
    log_date DATE NOT NULL COMMENT '打卡日期',
    is_period TINYINT DEFAULT 0 COMMENT '是否经期: 0-否 1-是',
    flow_level TINYINT COMMENT '流量: 1-少 2-中 3-多',
    pain_level TINYINT COMMENT '疼痛: 0-无 1-轻 2-中 3-重',
    symptoms VARCHAR(500) COMMENT '症状,逗号分隔',
    mood VARCHAR(100) COMMENT '心情',
    notes TEXT COMMENT '备注',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_user_date (user_id, log_date),
    INDEX idx_user_id (user_id),
    INDEX idx_log_date (log_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='每日经期打卡表';

-- =====================================================
-- 11. 系统配置表 (sys_config)
-- =====================================================
CREATE TABLE sys_config (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT 'ID',
    config_key VARCHAR(100) NOT NULL COMMENT '配置键',
    config_value TEXT COMMENT '配置值',
    description VARCHAR(255) COMMENT '描述',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_config_key (config_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统配置表';

-- =====================================================
-- 插入初始数据
-- =====================================================

-- 每日一言数据
INSERT INTO daily_quote (content, author, category) VALUES
('遇见你，是我这辈子最美的意外。', '', 1),
('我想和你一起，从清晨到日暮，从年少到白头。', '', 1),
('你是我的今天，也是我的明天。', '', 1),
('喜欢你，是我做过最正确的决定。', '', 1),
('世界很大，幸好有你。', '', 1),
('你是我藏在微风里的欢喜。', '', 1),
('余生很长，想和你浪费。', '', 1),
('你是我所有美好里的刚刚好。', '', 1),
('我想和你一起看遍世间风景。', '', 1),
('你是我平淡生活里的来日方长。', '', 1),
('山河远阔，人间烟火，无一是你，无一不是你。', '', 1),
('我想和你一起吃很多很多顿饭。', '', 1),
('你是我最想留住的幸运。', '', 1),
('遇见你之后，我再也没羡慕过别人。', '', 1),
('我想和你一起慢慢变老。', '', 1);

-- =====================================================
-- 11. 餐食记录表 (meal_record)
-- =====================================================
CREATE TABLE meal_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '记录ID',
    couple_id BIGINT NOT NULL COMMENT '情侣关系ID',
    user_id BIGINT NOT NULL COMMENT '下单人ID',
    meal_date DATE NOT NULL COMMENT '用餐日期',
    dishes JSON NOT NULL COMMENT '菜品快照[{menuItemId,name,imageUrl,count}]',
    rating TINYINT COMMENT '评分1-5星',
    comment VARCHAR(500) COMMENT '文字评价',
    review_by BIGINT COMMENT '评价人ID',
    review_time DATETIME COMMENT '评价时间',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_couple_id (couple_id),
    INDEX idx_meal_date (meal_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='餐食记录表';

-- =====================================================
-- 12. 衣物分类字典表 (wardrobe_category)
-- =====================================================
CREATE TABLE wardrobe_category (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    code VARCHAR(32) NOT NULL UNIQUE,
    name VARCHAR(32) NOT NULL,
    sort_order INT DEFAULT 0,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='衣物分类字典';

INSERT INTO wardrobe_category (code, name, sort_order) VALUES
  ('top','上衣',1),('bottom','下装',2),('coat','外套',3),
  ('shoes','鞋子',4),('bag','包配饰',5),('inner','内搭',6),('home','家居',7);

-- =====================================================
-- 13. 衣物表 (wardrobe_item)
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='衣物表';

-- =====================================================
-- 14. 穿搭方案表 (outfit)
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='穿搭方案表';

-- =====================================================
-- 15. 穿搭方案衣物关联表 (outfit_item)
-- =====================================================
CREATE TABLE outfit_item (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    outfit_id BIGINT NOT NULL,
    wardrobe_item_id BIGINT NOT NULL,
    slot VARCHAR(32),
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_outfit_item_outfit (outfit_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='穿搭方案衣物关联表';

-- 系统配置
INSERT INTO sys_config (config_key, config_value, description) VALUES
('wx.appid', '', '微信小程序AppID'),
('wx.secret', '', '微信小程序Secret'),
('wx.template.period_remind', '', '生理期提醒模板ID'),
('file.upload.path', '/app/uploads', '文件上传路径'),
('file.access.url', '/uploads', '文件访问URL前缀');
