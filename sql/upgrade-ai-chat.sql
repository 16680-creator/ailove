CREATE TABLE IF NOT EXISTS chat_message (
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
