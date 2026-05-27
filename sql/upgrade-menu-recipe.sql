-- 升级：为 menu_item 表添加 AI 做法字段
ALTER TABLE menu_item ADD COLUMN recipe TEXT COMMENT 'AI生成的做法' AFTER description;
