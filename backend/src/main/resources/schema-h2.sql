-- H2 兼容建表脚本
-- MODE=MySQL

CREATE TABLE IF NOT EXISTS sys_user (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    openid VARCHAR(100) UNIQUE NOT NULL,
    unionid VARCHAR(100),
    login_name VARCHAR(50) UNIQUE,
    password_hash VARCHAR(255),
    nickname VARCHAR(50),
    avatar_url VARCHAR(500),
    gender TINYINT DEFAULT 0,
    phone VARCHAR(20),
    birthday DATE,
    couple_id BIGINT,
    role TINYINT,
    status TINYINT DEFAULT 1,
    last_login_time TIMESTAMP,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS couple_link (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user1_id BIGINT NOT NULL,
    user2_id BIGINT,
    love_start_date DATE NOT NULL,
    love_motto VARCHAR(255),
    couple_photo VARCHAR(500),
    invite_code VARCHAR(20) UNIQUE,
    status TINYINT DEFAULT 0,
    bind_time TIMESTAMP,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS daily_quote (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    content VARCHAR(500) NOT NULL,
    author VARCHAR(50),
    category TINYINT DEFAULT 1,
    use_count INT DEFAULT 0,
    source TINYINT DEFAULT 0,
    couple_id BIGINT,
    quote_date DATE,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS menu_item (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    couple_id BIGINT NOT NULL,
    name VARCHAR(100) NOT NULL,
    image_url VARCHAR(500),
    category TINYINT DEFAULT 1,
    tags VARCHAR(200),
    difficulty TINYINT DEFAULT 3,
    cook_time INT,
    description CLOB,
    recipe CLOB,
    create_by BIGINT,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS diary (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    couple_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    title VARCHAR(200),
    content CLOB,
    mood TINYINT,
    weather VARCHAR(20),
    location VARCHAR(100),
    images CLOB,
    is_favorite TINYINT DEFAULT 0,
    view_count INT DEFAULT 0,
    diary_date DATE,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS photo_album (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    couple_id BIGINT NOT NULL,
    name VARCHAR(100) NOT NULL,
    cover_url VARCHAR(500),
    description VARCHAR(500),
    photo_count INT DEFAULT 0,
    sort_order INT DEFAULT 0,
    create_by BIGINT,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS photo (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    album_id BIGINT NOT NULL,
    couple_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    url VARCHAR(500) NOT NULL,
    thumbnail_url VARCHAR(500),
    description VARCHAR(500),
    location VARCHAR(100),
    shoot_time TIMESTAMP,
    file_size BIGINT,
    width INT,
    height INT,
    is_favorite TINYINT DEFAULT 0,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS wish_list (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    couple_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    title VARCHAR(200) NOT NULL,
    description CLOB,
    image_url VARCHAR(500),
    category TINYINT DEFAULT 1,
    priority TINYINT DEFAULT 3,
    status TINYINT DEFAULT 0,
    target_date DATE,
    complete_date DATE,
    complete_by BIGINT,
    linked_diary_id BIGINT,
    linked_photo_ids CLOB,
    sort_order INT DEFAULT 0,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS period_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    couple_id BIGINT NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE,
    cycle_days INT DEFAULT 28,
    period_days INT DEFAULT 5,
    symptoms VARCHAR(500),
    mood VARCHAR(100),
    flow_level TINYINT,
    pain_level TINYINT,
    notes CLOB,
    is_predicted TINYINT DEFAULT 0,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS period_daily_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    couple_id BIGINT NOT NULL,
    log_date DATE NOT NULL,
    is_period TINYINT DEFAULT 0,
    flow_level TINYINT,
    pain_level TINYINT,
    symptoms VARCHAR(500),
    mood VARCHAR(100),
    notes CLOB,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS meal_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    couple_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    meal_date DATE NOT NULL,
    dishes CLOB NOT NULL,
    rating TINYINT,
    comment VARCHAR(500),
    review_by BIGINT,
    review_time TIMESTAMP,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS trip_plan (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    couple_id BIGINT,
    from_city VARCHAR(50),
    to_city VARCHAR(50),
    start_date VARCHAR(20),
    end_date VARCHAR(20),
    preferences VARCHAR(100),
    budget VARCHAR(20),
    custom_request VARCHAR(500),
    status TINYINT DEFAULT 0,
    result_json CLOB,
    error_msg VARCHAR(500),
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS sys_config (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    config_key VARCHAR(100) NOT NULL UNIQUE,
    config_value CLOB,
    description VARCHAR(255),
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
