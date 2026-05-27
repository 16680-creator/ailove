-- H2 初始数据

-- 每日一言
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

-- 测试用户 (方便本地调试)
INSERT INTO sys_user (id, openid, nickname, avatar_url, gender, status) VALUES
(1, 'test_openid_boy', '小明', '', 1, 1),
(2, 'test_openid_girl', '小红', '', 2, 1);

-- 测试情侣关系
INSERT INTO couple_link (id, user1_id, user2_id, love_start_date, love_motto, invite_code, status, bind_time) VALUES
(1, 1, 2, '2024-01-01', '执子之手，与子偕老', 'ABC123', 1, CURRENT_TIMESTAMP);

-- 更新用户的情侣关系
UPDATE sys_user SET couple_id = 1, role = 1 WHERE id = 1;
UPDATE sys_user SET couple_id = 1, role = 2 WHERE id = 2;

-- 测试菜品数据
INSERT INTO menu_item (couple_id, name, category, difficulty, cook_time, description, create_by) VALUES
(1, '番茄炒蛋', 1, 1, 10, '经典家常菜，酸甜可口', 1),
(1, '红烧排骨', 1, 3, 45, '色泽红亮，肉质酥烂', 1),
(1, '可乐鸡翅', 1, 2, 30, '甜咸适中，小朋友最爱', 2),
(1, '意大利面', 2, 2, 20, '经典西式美味', 2),
(1, '珍珠奶茶', 5, 1, 15, '甜蜜的下午茶', 1),
(1, '蛋炒饭', 1, 1, 10, '简单又美味', 1);

-- 测试日记数据
INSERT INTO diary (couple_id, user_id, title, content, mood, weather, diary_date) VALUES
(1, 1, '在一起的第一天', '今天我们正式在一起了，好开心！', 1, '晴天', '2024-01-01'),
(1, 2, '做了好吃的', '今天给他做了番茄炒蛋，虽然有点糊了，但他说很好吃~', 1, '多云', '2024-01-05'),
(1, 1, '一起看了电影', '今天一起看了一部很感人的电影，她哭得稀里哗啦', 2, '阴天', '2024-01-10');

-- 测试心愿数据
INSERT INTO wish_list (couple_id, user_id, title, description, category, priority, status) VALUES
(1, 1, '一起去海边', '找个周末一起去看海', 1, 3, 0),
(1, 2, '养一只猫', '想养一只橘猫', 5, 2, 0),
(1, 1, '学做蛋糕', '一起学做生日蛋糕', 4, 1, 1),
(1, 2, '看一场日出', '早起一次看日出', 4, 3, 2);

-- 测试餐食记录
INSERT INTO meal_record (couple_id, user_id, meal_date, dishes, rating, comment, review_by, review_time) VALUES
(1, 1, CURRENT_DATE, '[{"menuItemId":1,"name":"番茄炒蛋","imageUrl":"","count":1},{"menuItemId":3,"name":"可乐鸡翅","imageUrl":"","count":2}]', 5, '今天的番茄炒蛋超级好吃！', 2, CURRENT_TIMESTAMP),
(1, 2, DATEADD('DAY', -1, CURRENT_DATE), '[{"menuItemId":4,"name":"意大利面","imageUrl":"","count":1},{"menuItemId":5,"name":"珍珠奶茶","imageUrl":"","count":1}]', NULL, NULL, NULL, NULL),
(1, 1, DATEADD('DAY', -2, CURRENT_DATE), '[{"menuItemId":2,"name":"红烧排骨","imageUrl":"","count":1},{"menuItemId":6,"name":"蛋炒饭","imageUrl":"","count":1}]', 4, '排骨做得不错，下次少放点盐', 1, CURRENT_TIMESTAMP);

-- 系统配置
INSERT INTO sys_config (config_key, config_value, description) VALUES
('wx.appid', '', '微信小程序AppID'),
('wx.secret', '', '微信小程序Secret'),
('file.upload.path', './uploads', '文件上传路径'),
('file.access.url', '/uploads', '文件访问URL前缀');
