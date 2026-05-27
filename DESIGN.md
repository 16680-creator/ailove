# 爱在朝夕 (AI Love Daily) — 项目设计文档

> 最后更新：2026-05-27

---

## 目录

1. [项目概述](#1-项目概述)
2. [技术架构](#2-技术架构)
3. [项目结构](#3-项目结构)
4. [功能模块说明](#4-功能模块说明)
5. [数据库设计](#5-数据库设计)
6. [API 接口列表](#6-api-接口列表)
7. [前端设计](#7-前端设计)
8. [AI 能力集成](#8-ai-能力集成)
9. [部署说明](#9-部署说明)
10. [安全注意事项](#10-安全注意事项)

---

## 1. 项目概述

### 1.1 产品定位

"爱在朝夕"是一款面向情侣的**生活记录微信小程序**，帮助情侣记录日常生活、管理共同心愿、追踪经期健康、规划旅行行程，并通过 AI 生成个性化内容。

### 1.2 核心功能

| 功能 | 说明 |
|------|------|
| 情侣绑定 | 通过邀请码将两个用户绑定为情侣关系 |
| 恋爱计时 | 显示在一起天数、纪念日倒计时 |
| 情侣日记 | 共享日记，支持心情、天气、图片 |
| 相册管理 | 分类相册，支持收藏、瀑布流浏览 |
| 今天吃什么 | 菜品库 + 随机推荐 + 下单记录 + 评价 + AI 菜谱 |
| 心愿清单 | 共享心愿列表，支持分类、优先级、完成状态 |
| 经期追踪 | 经期记录、每日打卡、周期预测、伴侣提醒 |
| 旅行规划 | AI 生成个性化旅行行程 |
| 每日情话 | AI 生成的情侣专属情话 |

### 1.3 目标用户

正在恋爱中的情侣，尤其是异地恋情侣，用于增强日常互动和共同记录生活。

---

## 2. 技术架构

### 2.1 整体架构

```
┌─────────────────────────────────────────────────────────┐
│                    微信小程序 (前端)                       │
│           JavaScript ES6 + Vant Weapp UI                 │
└──────────────────────┬──────────────────────────────────┘
                       │ HTTP / JSON
                       ▼
┌─────────────────────────────────────────────────────────┐
│               Spring Boot 后端 (Java 8)                  │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌─────────┐ │
│  │Controller│→ │ Service  │→ │  Mapper  │→ │ Database│ │
│  │  (12个)   │  │  (14个)  │  │  (13个)  │  │         │ │
│  └──────────┘  └──────────┘  └──────────┘  └─────────┘ │
│       │              │                                  │
│       │         ┌────┴────┐                             │
│       │         │ AI 服务  │                             │
│       │         │MiniMax  │                             │
│       │         └─────────┘                             │
│       ▼                                                 │
│  ┌──────────┐  ┌──────────┐                             │
│  │  Redis   │  │ 文件存储  │                             │
│  │  缓存层   │  │ uploads/ │                             │
│  └──────────┘  └──────────┘                             │
└─────────────────────────────────────────────────────────┘
                       │
          ┌────────────┼────────────┐
          ▼            ▼            ▼
    ┌──────────┐ ┌──────────┐ ┌──────────┐
    │  MySQL   │ │  H2 DB   │ │  Redis   │
    │  (生产)   │ │  (开发)   │ │  (缓存)   │
    └──────────┘ └──────────┘ └──────────┘
```

### 2.2 技术栈

| 层级 | 技术 | 版本 |
|------|------|------|
| **后端框架** | Spring Boot | 2.7.18 |
| **编程语言** | Java | 8 (JDK 1.8) |
| **ORM** | MyBatis-Plus | 3.5.5 |
| **生产数据库** | MySQL | 8.0 |
| **开发数据库** | H2 (文件模式, MySQL 兼容) | 内嵌 |
| **缓存** | Redis (Lettuce 客户端) | 7.x |
| **认证** | JWT (jjwt) | 0.9.1 |
| **密码加密** | BCrypt (spring-security-crypto) | 内置 |
| **API 文档** | Knife4j (OpenAPI 3) | 4.1.0 |
| **工具库** | Hutool | 5.8.23 |
| **构建工具** | Maven | 3.6+ |
| **前端平台** | 微信小程序 | 基础库 2.25.0 |
| **前端语言** | JavaScript (ES6) | — |
| **前端 UI** | Vant Weapp | 1.10.18 |
| **AI 集成** | MiniMax API (MiniMax-M2.7 模型) | — |
| **容器化** | Docker + Docker Compose | 3.8 |
| **反向代理** | Caddy (生产环境) | — |
| **自动化部署** | MCP Deploy Server (ssh2) | 1.12.1 |

### 2.3 Spring Profile 策略

| Profile | 数据库 | 用途 | 特点 |
|---------|--------|------|------|
| `local` | H2 文件模式 | 本地开发 | 调试日志, AI 默认开启, Knife4j 开启 |
| `prod` | MySQL | 生产部署 | 信息日志, AI 默认关闭, Knife4j 关闭 |
| 默认 | MySQL (远程) | 通用 | 直连远程 MySQL |

---

## 3. 项目结构

```
ai-love-daily/
├── backend/                          # 后端 Spring Boot 项目
│   ├── Dockerfile                    # Docker 构建文件
│   ├── pom.xml                       # Maven 依赖配置
│   └── src/main/
│       ├── java/com/ailovedaily/
│       │   ├── AiLoveDailyApplication.java   # 启动类
│       │   ├── config/               # 配置类 (6个)
│       │   │   ├── AsyncConfig.java          # 异步线程池
│       │   │   ├── JwtConfig.java            # JWT 拦截器注册
│       │   │   ├── MyBatisPlusConfig.java    # 分页 + 自动填充
│       │   │   ├── PasswordConfig.java       # BCrypt 编码器
│       │   │   ├── RedisConfig.java          # Redis 序列化
│       │   │   └── WebMvcConfig.java         # CORS + 静态资源
│       │   ├── controller/           # 控制器 (12个)
│       │   ├── dto/                  # 数据传输对象 (13个)
│       │   ├── entity/               # 实体类 (13个)
│       │   ├── exception/            # 异常处理 (5个)
│       │   ├── interceptor/          # JWT 拦截器
│       │   ├── mapper/               # MyBatis Mapper (13个)
│       │   ├── service/              # 服务接口 (14个)
│       │   │   └── impl/             # 服务实现 (14个)
│       │   ├── task/                 # 定时任务 (2个)
│       │   ├── utils/                # JWT 工具类
│       │   └── vo/                   # 视图对象 (13个)
│       └── resources/
│           ├── application.yml       # 主配置
│           ├── application-local.yml # 本地开发配置
│           ├── application-prod.yml  # 生产配置
│           ├── schema-h2.sql         # H2 建表语句
│           └── data-h2.sql           # H2 种子数据
│
├── frontend/                         # 微信小程序前端
│   ├── app.js / app.json / app.wxss  # 应用入口
│   ├── components/vant-weapp/        # Vant Weapp 组件库 (70+)
│   ├── images/                       # 静态图片资源
│   ├── utils/                        # 工具函数
│   │   ├── request.js                # HTTP 请求封装
│   │   └── util.js                   # 日期/数组工具
│   └── pages/                        # 页面 (10个)
│       ├── index/                    # 首页
│       ├── register/                 # 注册页
│       ├── diary/                    # 日记列表 + 详情
│       ├── album/                    # 相册列表 + 照片
│       ├── menu/                     # 今天吃什么
│       ├── menu-detail/              # 菜品详情 + AI 菜谱
│       ├── wish/                     # 心愿清单 + 详情
│       ├── period/                   # 经期追踪
│       ├── trip-plan/                # 旅行规划
│       └── profile/                  # 个人中心
│
├── sql/                              # 数据库脚本
│   ├── init.sql                      # MySQL 初始化 (12表)
│   ├── upgrade-ai-quote.sql          # AI 情话功能迁移
│   ├── upgrade-menu-recipe.sql       # 菜谱功能迁移
│   └── upgrade-web-auth.sql          # Web 登录迁移
│
├── deploy/                           # 部署配置
│   ├── docker-compose.yml            # Docker 编排
│   ├── .env.example                  # 环境变量模板
│   ├── start-backend.sh / .bat       # 启动脚本
│   ├── stop-backend.sh / .bat        # 停止脚本
│   ├── linux-prod/                   # Linux 生产包
│   │   ├── app.jar                   # 预构建 JAR (~52MB)
│   │   ├── start.sh / stop.sh        # 启停脚本
│   │   └── .env.example              # 环境变量模板
│   └── mcp/                          # MCP 自动部署服务器
│       ├── index.mjs                 # 部署工具主逻辑
│       ├── setup-caddy.mjs           # Caddy 反向代理配置
│       └── package.json              # Node.js 依赖
│
├── data/                             # H2 运行时数据
└── uploads/                          # 文件上传存储
```

---

## 4. 功能模块说明

### 4.1 认证模块 (Auth)

**职责**: 用户登录、注册、Token 管理

- **微信登录**: 通过 `wx.login()` 获取 code，后端调用微信 `jscode2session` 接口换取 openid，签发 JWT
- **账号登录**: 支持 loginName + password 的 Web 端登录方式 (BCrypt 加密)
- **注册**: 创建账号 + 自动登录，返回 token 和状态标识 (isNewUser, hasCouple)
- **开发模式**: `/auth/dev-login` 跳过微信认证，直接按 userId 登录

**JWT 机制**: HS512 算法，默认 30 天过期，通过 `Authorization: Bearer <token>` 传递。拦截器从 token 中提取 userId、openid、coupleId 注入请求属性。

### 4.2 情侣关系模块 (Couple)

**职责**: 情侣绑定、关系管理

- **创建关系**: 一方发起，生成 6 位随机邀请码
- **邀请绑定**: 另一方输入邀请码完成绑定，设置双方 couple_id 和 role
- **恋爱信息**: 计算在一起天数、下一个纪念日倒计时、恋爱宣言、情侣合照
- **解绑**: 双方 user.couple_id 清空，关系状态设为 unbound

### 4.3 首页模块 (Home)

**职责**: 聚合展示情侣动态

返回 `HomeVO` 包含：用户信息、伴侣信息、恋爱信息、每日情话(AI 或随机)、快捷统计(日记/照片/心愿/菜品数量)、最近日记(3条)、最近照片(6张)

### 4.4 日记模块 (Diary)

**职责**: 情侣共享日记

- 支持标题、正文、心情(5种: 开心/感动/平静/难过/生气)、天气、位置、图片(JSON 数组)
- 分页查询、时间线视图、收藏/取消收藏、浏览计数自增
- 所有权校验：只能修改/删除自己的日记
- 批量加载用户信息避免 N+1 查询

### 4.5 相册模块 (Album + Photo)

**职责**: 照片管理

- 相册 CRUD，支持封面图、描述、排序、照片计数
- 单张/批量上传照片，自动生成缩略图 (最大 800px 宽度，使用 Java ImageIO)
- 瀑布流浏览 (全部照片)、收藏功能、分页查询
- 删除相册时级联删除所有照片文件

### 4.6 今天吃什么模块 (Menu + MealRecord)

**职责**: 共享菜品库 + 点餐记录

- **菜品管理**: 名称、图片、分类(家常菜/西餐/小吃/甜点/饮品)、标签、难度(1-5星)、烹饪时间
- **AI 菜谱**: 添加菜品时异步触发 MiniMax 生成菜谱，详情页轮询等待 (2秒/次，最多60秒)
- **点餐界面**: 外卖风格，左侧分类导航 + 右侧菜品列表，底部购物车
- **甜蜜任务**: 下单后随机抽取 15 种甜蜜任务之一(亲吻/拥抱/按摩/说爱你等)
- **评价系统**: 每条记录一次评价机会，1-5 星 + 文字点评
- **历史记录**: 按日期分组的最近 N 天点餐记录

### 4.7 心愿清单模块 (Wish)

**职责**: 共享心愿管理

- **分类**: 旅行/美食/购物/体验/其他
- **优先级**: 低/中/高/紧急 (颜色编码)
- **状态**: 待实现/进行中/已完成/已放弃
- 完成心愿时可关联日记和照片
- 状态统计面板 (总数/待实现/已完成)

### 4.8 经期追踪模块 (Period)

**职责**: 女性经期健康管理

- **周期记录**: 记录经期开始/结束日期、周期天数、经期天数、症状、心情、流量、疼痛等级
- **每日打卡**: 经期状态、流量(少/中/多)、疼痛(无/轻/中/重)、症状、心情、备注
- **智能预测**: 基于历史实际记录计算平均周期/经期天数，自动预测未来 6 次经期
- **状态判断**: 安全期/排卵期/经期 三种状态
- **伴侣提醒**: 定时任务 (每天 8:00) 检查即将到来的经期，记录提醒日志
- **日历视图**: 自定义月历展示经期/打卡状态
- **Redis 缓存**: 经期信息缓存提升性能
- **性别限制**: 仅女性用户 (gender=2) 可记录

### 4.9 旅行规划模块 (TripPlan)

**职责**: AI 旅行行程生成

- **输入**: 出发城市、目的地、日期、偏好(美食/文艺/自然/历史/购物/浪漫)、预算(经济/适中/宽松)、自定义需求
- **异步生成**: 提交后后台调用 MiniMax AI 生成，前端每 3 秒轮询结果
- **天气集成**: 通过 wttr.in 获取目的地天气预报，嵌入 AI 提示词
- **结构化输出**: 按天展示行程 — 时间、类型(景点/餐饮/交通/住宿/购物/休闲)、标题、描述、提示、天气
- **历史记录**: 查看/删除过往规划

### 4.10 每日情话模块 (AiQuote)

**职责**: AI 个性化情话生成

- **三级缓存**: Redis → 数据库 → AI API 调用
- **个性化提示词**: 使用情侣昵称、在一起天数、当前季节构建 prompt
- **定时预生成**: 每天早上 6:00 (`AiDailyQuoteTask`) 为所有已绑定情侣批量生成
- **手动刷新**: 用户可调用 `/home/ai-quote/refresh` 重新生成
- **输出清洗**: 移除 AI 思维链标签、Markdown 格式、提示词泄露内容

### 4.11 文件上传模块 (File)

**职责**: 文件/图片上传

- 按日期目录 (`yyyy/MM/dd/`) 存储，UUID 文件名防冲突
- 图片上传自动生成缩略图 (最大宽度 800px)
- 静态资源映射：`/uploads/**` 可直接访问

---

## 5. 数据库设计

### 5.1 ER 关系图

```
┌──────────┐     1:1     ┌─────────────┐     1:1     ┌──────────┐
│ sys_user │────────────→│ couple_link │←────────────│ sys_user │
│  (用户A)  │  couple_id  │             │  user2_id   │  (用户B)  │
└──────────┘             └─────────────┘             └──────────┘
     │                         │
     │ couple_id               │ couple_id
     ▼                         ▼
┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────┐
│  diary   │  │photo_album│  │wish_list │  │menu_item │  │meal_record│
└──────────┘  └──────────┘  └──────────┘  └──────────┘  └──────────┘
                   │
                   │ album_id
                   ▼
              ┌──────────┐  ┌──────────────┐  ┌─────────────────┐
              │  photo   │  │ period_record │  │ period_daily_log│
              └──────────┘  └──────────────┘  └─────────────────┘

┌─────────────┐  ┌────────────┐  ┌────────────┐
│ daily_quote │  │ sys_config │  │ trip_plan  │
└─────────────┘  └────────────┘  └────────────┘
```

**核心设计**: `couple_link` 是几乎所有业务表的外键关联点，体现了"情侣为中心"的数据模型。所有表间关系通过应用层维护，未定义数据库级 `FOREIGN KEY` 约束。

### 5.2 表结构详细说明

#### sys_user (用户表)

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGINT | PK, 自增 | 用户 ID |
| openid | VARCHAR(100) | UNIQUE, NOT NULL | 微信 OpenID |
| unionid | VARCHAR(100) | | 微信 UnionID |
| login_name | VARCHAR(50) | UNIQUE | Web 登录账号 |
| password_hash | VARCHAR(255) | | Web 登录密码 (BCrypt) |
| nickname | VARCHAR(50) | | 昵称 |
| avatar_url | VARCHAR(500) | | 头像 URL |
| gender | TINYINT | DEFAULT 0 | 0=未知, 1=男, 2=女 |
| phone | VARCHAR(20) | | 手机号 |
| birthday | DATE | | 生日 |
| couple_id | BIGINT | | 情侣关系 ID |
| role | TINYINT | | 1=发起方, 2=接受方 |
| status | TINYINT | DEFAULT 1 | 0=禁用, 1=正常 |
| last_login_time | DATETIME | | 最后登录时间 |
| create_time | DATETIME | DEFAULT NOW() | 创建时间 |
| update_time | DATETIME | ON UPDATE NOW() | 更新时间 |

索引: `idx_openid`, `idx_login_name`, `idx_couple_id`

#### couple_link (情侣关系表)

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGINT | PK, 自增 | 关系 ID |
| user1_id | BIGINT | NOT NULL | 用户 1 ID (发起方) |
| user2_id | BIGINT | | 用户 2 ID (接受方) |
| love_start_date | DATE | NOT NULL | 恋爱开始日期 |
| love_motto | VARCHAR(255) | | 恋爱宣言 |
| couple_photo | VARCHAR(500) | | 情侣合照 URL |
| invite_code | VARCHAR(20) | UNIQUE | 邀请码 (6位) |
| status | TINYINT | DEFAULT 0 | 0=待绑定, 1=已绑定, 2=已解绑 |
| bind_time | DATETIME | | 绑定时间 |
| create_time | DATETIME | DEFAULT NOW() | |
| update_time | DATETIME | ON UPDATE NOW() | |

索引: `idx_user1`, `idx_user2`, `idx_invite_code`

#### daily_quote (每日情话表)

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGINT | PK, 自增 | ID |
| content | VARCHAR(500) | NOT NULL | 情话内容 |
| author | VARCHAR(50) | | 作者 |
| category | TINYINT | DEFAULT 1 | 1=情话, 2=励志, 3=幽默 |
| use_count | INT | DEFAULT 0 | 使用次数 |
| source | TINYINT | DEFAULT 0 | 0=手动, 1=AI 生成 |
| couple_id | BIGINT | | 关联情侣 ID (AI 个性化) |
| quote_date | DATE | | 生效日期 (AI 每日缓存) |
| create_time | DATETIME | DEFAULT NOW() | |

索引: `idx_category`, `idx_couple_date`, `idx_source`

#### diary (日记表)

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGINT | PK, 自增 | 日记 ID |
| couple_id | BIGINT | NOT NULL | 情侣关系 ID |
| user_id | BIGINT | NOT NULL | 作者 ID |
| title | VARCHAR(200) | | 标题 |
| content | TEXT | | 内容 |
| mood | TINYINT | | 1=开心, 2=感动, 3=平静, 4=难过, 5=生气 |
| weather | VARCHAR(20) | | 天气 |
| location | VARCHAR(100) | | 位置 |
| images | JSON | | 图片 URL 数组 |
| is_favorite | TINYINT | DEFAULT 0 | 是否收藏 |
| view_count | INT | DEFAULT 0 | 浏览次数 |
| diary_date | DATE | | 日记日期 |
| create_time | DATETIME | DEFAULT NOW() | |
| update_time | DATETIME | ON UPDATE NOW() | |

索引: `idx_couple_id`, `idx_user_id`, `idx_diary_date`

#### photo_album (相册表)

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGINT | PK, 自增 | 相册 ID |
| couple_id | BIGINT | NOT NULL | 情侣关系 ID |
| name | VARCHAR(100) | NOT NULL | 相册名称 |
| cover_url | VARCHAR(500) | | 封面图 URL |
| description | VARCHAR(500) | | 描述 |
| photo_count | INT | DEFAULT 0 | 照片数量 |
| sort_order | INT | DEFAULT 0 | 排序 |
| create_by | BIGINT | | 创建者 ID |
| create_time | DATETIME | DEFAULT NOW() | |
| update_time | DATETIME | ON UPDATE NOW() | |

索引: `idx_couple_id`

#### photo (照片表)

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGINT | PK, 自增 | 照片 ID |
| album_id | BIGINT | NOT NULL | 相册 ID |
| couple_id | BIGINT | NOT NULL | 情侣关系 ID |
| user_id | BIGINT | NOT NULL | 上传者 ID |
| url | VARCHAR(500) | NOT NULL | 图片 URL |
| thumbnail_url | VARCHAR(500) | | 缩略图 URL |
| description | VARCHAR(500) | | 描述 |
| location | VARCHAR(100) | | 拍摄地点 |
| shoot_time | DATETIME | | 拍摄时间 |
| file_size | BIGINT | | 文件大小 (字节) |
| width | INT | | 图片宽度 |
| height | INT | | 图片高度 |
| is_favorite | TINYINT | DEFAULT 0 | 是否收藏 |
| create_time | DATETIME | DEFAULT NOW() | |

索引: `idx_album_id`, `idx_couple_id`, `idx_user_id`

#### menu_item (菜品表)

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGINT | PK, 自增 | 菜品 ID |
| couple_id | BIGINT | NOT NULL | 情侣关系 ID |
| name | VARCHAR(100) | NOT NULL | 菜品名称 |
| image_url | VARCHAR(500) | | 图片 URL |
| category | TINYINT | DEFAULT 1 | 1=家常菜, 2=西餐, 3=小吃, 4=甜点, 5=饮品 |
| tags | VARCHAR(200) | | 标签 (逗号分隔) |
| difficulty | TINYINT | DEFAULT 3 | 难度 1-5 星 |
| cook_time | INT | | 烹饪时间 (分钟) |
| description | TEXT | | 描述 |
| recipe | TEXT | | AI 生成的菜谱 |
| create_by | BIGINT | | 创建者 ID |
| create_time | DATETIME | DEFAULT NOW() | |
| update_time | DATETIME | ON UPDATE NOW() | |

索引: `idx_couple_id`, `idx_category`

#### meal_record (点餐记录表)

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGINT | PK, 自增 | 记录 ID |
| couple_id | BIGINT | NOT NULL | 情侣关系 ID |
| user_id | BIGINT | NOT NULL | 下单者 ID |
| meal_date | DATE | NOT NULL | 用餐日期 |
| dishes | JSON | NOT NULL | 菜品快照 `[{menuItemId, name, imageUrl, count}]` |
| rating | TINYINT | | 评价 1-5 星 |
| comment | VARCHAR(500) | | 文字点评 |
| review_by | BIGINT | | 评价者 ID |
| review_time | DATETIME | | 评价时间 |
| create_time | DATETIME | DEFAULT NOW() | |
| update_time | DATETIME | ON UPDATE NOW() | |

索引: `idx_couple_id`, `idx_meal_date`

#### wish_list (心愿清单表)

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGINT | PK, 自增 | 心愿 ID |
| couple_id | BIGINT | NOT NULL | 情侣关系 ID |
| user_id | BIGINT | NOT NULL | 创建者 ID |
| title | VARCHAR(200) | NOT NULL | 心愿标题 |
| description | TEXT | | 描述 |
| image_url | VARCHAR(500) | | 图片 |
| category | TINYINT | DEFAULT 1 | 1=旅行, 2=美食, 3=购物, 4=体验, 5=其他 |
| priority | TINYINT | DEFAULT 3 | 1=低, 2=中, 3=高, 4=紧急 |
| status | TINYINT | DEFAULT 0 | 0=待实现, 1=进行中, 2=已完成, 3=已放弃 |
| target_date | DATE | | 目标完成日期 |
| complete_date | DATE | | 实际完成日期 |
| complete_by | BIGINT | | 完成者 ID |
| linked_diary_id | BIGINT | | 关联日记 ID |
| linked_photo_ids | JSON | | 关联照片 ID 数组 |
| sort_order | INT | DEFAULT 0 | 排序 |
| create_time | DATETIME | DEFAULT NOW() | |
| update_time | DATETIME | ON UPDATE NOW() | |

索引: `idx_couple_id`, `idx_user_id`, `idx_status`, `idx_category`

#### period_record (经期记录表)

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGINT | PK, 自增 | 记录 ID |
| user_id | BIGINT | NOT NULL | 女性用户 ID |
| couple_id | BIGINT | NOT NULL | 情侣关系 ID |
| start_date | DATE | NOT NULL | 开始日期 |
| end_date | DATE | | 结束日期 |
| cycle_days | INT | DEFAULT 28 | 周期天数 |
| period_days | INT | DEFAULT 5 | 经期天数 |
| symptoms | VARCHAR(500) | | 症状 (逗号分隔) |
| mood | VARCHAR(100) | | 心情 |
| flow_level | TINYINT | | 1=少, 2=中, 3=多 |
| pain_level | TINYINT | | 0=无, 1=轻, 2=中, 3=重 |
| notes | TEXT | | 备注 |
| is_predicted | TINYINT | DEFAULT 0 | 0=实际, 1=预测 |
| create_time | DATETIME | DEFAULT NOW() | |
| update_time | DATETIME | ON UPDATE NOW() | |

索引: `idx_user_id`, `idx_couple_id`, `idx_start_date`

#### period_daily_log (经期每日打卡表)

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGINT | PK, 自增 | 记录 ID |
| user_id | BIGINT | NOT NULL | 用户 ID |
| couple_id | BIGINT | NOT NULL | 情侣关系 ID |
| log_date | DATE | NOT NULL | 打卡日期 |
| is_period | TINYINT | DEFAULT 0 | 0=否, 1=是 |
| flow_level | TINYINT | | 1=少, 2=中, 3=多 |
| pain_level | TINYINT | | 0=无, 1=轻, 2=中, 3=重 |
| symptoms | VARCHAR(500) | | 症状 |
| mood | VARCHAR(100) | | 心情 |
| notes | TEXT | | 备注 |
| create_time | DATETIME | DEFAULT NOW() | |
| update_time | DATETIME | ON UPDATE NOW() | |

唯一键: `uk_user_date` (user_id, log_date)

#### trip_plan (旅行规划表)

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGINT | PK, 自增 | 规划 ID |
| user_id | BIGINT | NOT NULL | 用户 ID |
| couple_id | BIGINT | NOT NULL | 情侣关系 ID |
| from_city | VARCHAR(50) | | 出发城市 |
| to_city | VARCHAR(50) | | 目的地城市 |
| start_date | DATE | | 开始日期 |
| end_date | DATE | | 结束日期 |
| preferences | VARCHAR(200) | | 偏好 (逗号分隔) |
| budget | VARCHAR(50) | | 预算 |
| custom_request | TEXT | | 自定义需求 |
| status | TINYINT | DEFAULT 0 | 0=生成中, 1=已完成, 2=失败 |
| result_json | TEXT | | AI 生成结果 JSON |
| error_msg | VARCHAR(500) | | 错误信息 |
| create_time | DATETIME | DEFAULT NOW() | |
| update_time | DATETIME | ON UPDATE NOW() | |

#### sys_config (系统配置表)

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGINT | PK, 自增 | ID |
| config_key | VARCHAR(100) | NOT NULL | 配置键 |
| config_value | TEXT | | 配置值 |
| description | VARCHAR(255) | | 描述 |
| create_time | DATETIME | DEFAULT NOW() | |
| update_time | DATETIME | ON UPDATE NOW() | |

唯一键: `uk_config_key`

### 5.3 JSON 字段说明

| 表 | 字段 | 格式 |
|----|------|------|
| diary | images | `["url1", "url2", ...]` |
| wish_list | linked_photo_ids | `[1, 2, 3]` |
| meal_record | dishes | `[{"menuItemId":1, "name":"红烧肉", "imageUrl":"...", "count":1}]` |

### 5.4 种子数据

- **daily_quote**: 15 条情话 (category=1)
- **sys_config**: 5 条配置 (微信 appid/secret、经期提醒模板、文件上传路径、文件访问 URL)
- **H2 开发环境**: 测试用户 (小明 id=1 / 小红 id=2)、情侣关系、示例菜品、日记、心愿、点餐记录

---

## 6. API 接口列表

### 6.1 统一响应格式

```json
{
  "code": 200,
  "message": "success",
  "data": { ... }
}
```

错误码: `200` 成功, `400` 参数错误, `403` 未授权, `404` 资源不存在, `500` 服务器错误

### 6.2 认证接口 `/api/auth` (无需 JWT)

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/auth/login` | 微信小程序登录 (code 换 openid) |
| POST | `/api/auth/web-login` | 账号密码登录 (loginName + password) |
| POST | `/api/auth/register` | 账号注册 + 自动登录 |
| POST | `/api/auth/dev-login` | 开发环境登录 (跳过微信认证, userId 参数) |

### 6.3 用户接口 `/api/user` (需 JWT)

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/user/info` | 获取当前用户信息 (含伴侣和恋爱信息) |
| PUT | `/api/user/info` | 更新用户资料 (nickname, avatarUrl, gender, phone, birthday) |
| GET | `/api/user/partner` | 获取伴侣信息 |

### 6.4 情侣关系接口 `/api/couple` (需 JWT)

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/couple/create` | 创建情侣关系 (loveStartDate, loveMotto)，返回邀请码 |
| POST | `/api/couple/bind` | 通过邀请码绑定 |
| GET | `/api/couple/info` | 获取恋爱信息 (天数、宣言、纪念日倒计时、合照) |
| PUT | `/api/couple/motto` | 更新恋爱宣言 |
| POST | `/api/couple/unbind` | 解除绑定 |
| POST | `/api/couple/photo` | 上传情侣合照 |

### 6.5 首页接口 `/api/home` (需 JWT)

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/home/data` | 首页聚合数据 (HomeVO) |
| GET | `/api/home/quote` | 随机每日情话 |
| GET | `/api/home/ai-quote` | AI 生成的个性化情话 |
| POST | `/api/home/ai-quote/refresh` | 强制刷新 AI 情话 |

### 6.6 日记接口 `/api/diary` (需 JWT)

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/diary` | 创建日记 |
| PUT | `/api/diary/{id}` | 更新日记 |
| DELETE | `/api/diary/{id}` | 删除日记 |
| GET | `/api/diary` | 分页查询日记列表 (page, size) |
| GET | `/api/diary/timeline` | 时间线视图 |
| GET | `/api/diary/{id}` | 日记详情 (自增浏览计数) |
| POST | `/api/diary/{id}/favorite` | 收藏/取消收藏 |
| GET | `/api/diary/favorites` | 收藏日记列表 |

### 6.7 相册接口 `/api/album` (需 JWT)

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/album` | 创建相册 |
| PUT | `/api/album/{id}` | 更新相册 |
| DELETE | `/api/album/{id}` | 删除相册 (含所有照片文件) |
| GET | `/api/album/list` | 相册列表 |
| POST | `/api/album/{albumId}/photo` | 上传单张照片 |
| POST | `/api/album/{albumId}/photos` | 批量上传照片 |
| DELETE | `/api/album/photo/{id}` | 删除照片 |
| GET | `/api/album/{albumId}/photos` | 相册内照片分页 |
| GET | `/api/album/photos/all` | 全部照片 (瀑布流) |
| POST | `/api/album/photo/{id}/favorite` | 收藏/取消收藏 |
| GET | `/api/album/photos/favorites` | 收藏照片列表 |
| GET | `/api/album/photo/{id}` | 照片详情 |

### 6.8 菜品接口 `/api/menu` (需 JWT)

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/menu` | 添加菜品 (触发异步 AI 菜谱生成) |
| PUT | `/api/menu/{id}` | 更新菜品 |
| DELETE | `/api/menu/{id}` | 删除菜品 |
| GET | `/api/menu` | 分页查询 (page, size, category 可选) |
| GET | `/api/menu/category/{category}` | 按分类查询 |
| GET | `/api/menu/random` | 随机推荐一道菜 |
| GET | `/api/menu/{id}` | 菜品详情 (含 AI 菜谱) |
| POST | `/api/menu/{id}/recipe` | 手动触发 AI 菜谱生成 |

### 6.9 点餐记录接口 `/api/meal-record` (需 JWT)

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/meal-record` | 创建点餐记录 (含菜品列表) |
| POST | `/api/meal-record/{id}/review` | 添加评价 (rating + comment, 仅一次) |
| GET | `/api/meal-record/history` | 按日期分组的历史记录 (days 参数) |

### 6.10 心愿清单接口 `/api/wish` (需 JWT)

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/wish` | 添加心愿 |
| PUT | `/api/wish/{id}` | 更新心愿 |
| DELETE | `/api/wish/{id}` | 删除心愿 |
| POST | `/api/wish/{id}/complete` | 标记完成 (可关联 diaryId, photoIds) |
| POST | `/api/wish/{id}/uncomplete` | 撤回完成状态 |
| GET | `/api/wish` | 按状态筛选 (status 参数) |
| GET | `/api/wish/category/{category}` | 按分类筛选 |
| GET | `/api/wish/{id}` | 心愿详情 |
| GET | `/api/wish/stats` | 状态统计 (总数/待实现/已完成) |

### 6.11 经期追踪接口 `/api/period` (需 JWT)

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/period` | 记录经期 (自动重新生成预测) |
| PUT | `/api/period/{id}` | 更新经期记录 |
| DELETE | `/api/period/{id}` | 删除经期记录 |
| GET | `/api/period/info` | 经期概览 (平均周期、下次预测、当前状态、最近记录) |
| GET | `/api/period/records` | 最近经期记录列表 |
| POST | `/api/period/predict` | 手动重新生成预测 |
| POST | `/api/period/daily-log` | 保存/更新每日打卡 |
| GET | `/api/period/daily-log` | 月度打卡记录 (year, month 参数) |
| GET | `/api/period/daily-log/{date}` | 单日打卡详情 |

### 6.12 旅行规划接口 `/api/trip-plan` (需 JWT)

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/trip-plan/generate` | 提交异步 AI 旅行规划 (返回 planId) |
| GET | `/api/trip-plan/{id}` | 获取规划详情 (含 result JSON) |
| GET | `/api/trip-plan/list` | 规划历史列表 |
| DELETE | `/api/trip-plan/{id}` | 删除规划 |

### 6.13 文件上传接口 `/api/file` (需 JWT)

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/file/upload` | 通用文件上传 |
| POST | `/api/file/upload/image` | 图片上传 (自动生成缩略图, 最大 800px) |

**接口总计: 54 个，分布在 12 个 Controller 中**

---

## 7. 前端设计

### 7.1 页面导航结构

```
TabBar (4 个标签):
  ├── pages/index/index        (首页 - 恋爱计时、每日情话、快捷入口)
  ├── pages/diary/diary        (日记 - 列表 + 创建)
  ├── pages/album/album        (相册 - 列表 + 创建)
  └── pages/profile/profile    (我的 - 个人资料、情侣信息)

子页面 (navigateTo):
  ├── pages/register/register       (账号注册)
  ├── pages/menu/menu               (今天吃什么 - 外卖风格点餐)
  ├── pages/menu-detail/menu-detail (菜品详情 + AI 菜谱)
  ├── pages/diary/detail            (日记详情)
  ├── pages/album/photos            (相册照片浏览 + 上传)
  ├── pages/wish/wish               (心愿清单)
  ├── pages/wish/detail             (心愿详情)
  ├── pages/period/period           (经期追踪 + 日历 + 打卡)
  └── pages/trip-plan/trip-plan     (AI 旅行规划)
```

### 7.2 设计语言

- **色彩体系**: 奶油暖棕色调
  - `--accent: #bb775b` (主强调色)
  - `--accent-deep: #7f5342` (深强调色)
  - `--cream-base: #f7efe5` (背景底色)
  - `--cream-surface: #fbf4eb` (卡片表面)
  - `--text-primary: #34261e` (主文字)
- **视觉风格**: 杂志风排版 + 毛玻璃效果 (backdrop-filter: blur) + 渐变按钮
- **动画**: fadeIn (淡入)、pulse (脉搏)、float (漂浮) 三种 CSS 动画
- **组件库**: 全局注册 27 个 Vant Weapp 组件 (button, cell, icon, popup, toast, dialog, field, uploader, datetime-picker, picker, action-sheet, tag, progress, steps, calendar, empty, grid, card, skeleton, swipe-cell, tabs, tab, nav-bar, notice-bar, loading, image, cell-group)

### 7.3 状态管理

- **全局状态**: `app.globalData` — token, userInfo, baseUrl, devMode, devUserId
- **页面状态**: 各页面 `data` 对象 + `this.setData()`
- **持久存储**: `wx.getStorageSync` 仅存储 auth token 和 `trip_from_city`
- **无跨页面通信**: 页面独立，各自在 `onLoad`/`onShow` 从 API 加载数据

### 7.4 网络请求封装

`utils/request.js` 提供:
- `request(options)`: Promise 封装，自动附加 Bearer token，401 时清除 token 并提示重新登录
- `uploadFile(options)`: 文件上传封装，同样处理认证
- `fixUrl(url)`: 将后端返回的相对路径 (如 `/uploads/2026/05/27/xxx.jpg`) 转为绝对 URL

`utils/util.js` 提供:
- `formatDate(date, format)`: 日期格式化 (YYYY-MM-DD / MM-DD / YYYY年MM月DD日)
- `formatDays(startDate, endDate)`: 计算两个日期之间的天数
- `toSafeArray(val)`: 安全地将任意值转为数组

### 7.5 双登录模式

通过 `app.globalData.devMode` 切换:
- **开发模式** (`devMode: true`): 直接调用 `/auth/dev-login?userId=2` 跳过微信认证
- **生产模式** (`devMode: false`): 调用 `wx.login()` 获取 code，再请求 `/auth/login`

---

## 8. AI 能力集成

### 8.1 AI 模型

使用 **MiniMax M2.7** 大语言模型
- API 端点: `https://api.minimax.chat/v1/text/chatcompletion_v2`
- 用途: 情话生成、菜谱生成、旅行行程规划

### 8.2 AI 功能清单

| 功能 | 触发方式 | 缓存策略 | 个性化内容 |
|------|----------|----------|------------|
| 每日情话 | 定时任务 (每天 6:00) + 手动刷新 | Redis → DB → AI API (三级) | 情侣昵称、在一起天数、当前季节 |
| 菜品菜谱 | 添加菜品时异步触发 | 直接写入菜品 recipe 字段 | 菜品名称、描述、分类、难度 |
| 旅行行程 | 用户提交后异步生成 | 直接写入 trip_plan.result_json | 出发/目的地、日期、偏好、预算、天气 |

### 8.3 AI 输出清洗

所有 AI 返回内容经过清洗处理:
- 移除思维链标签 (`<think>...</think>`)
- 移除 Markdown 格式符号 (`#`, `*`, `-` 等)
- 移除提示词泄露内容 (检测并截断)
- JSON 提取 (旅行规划场景，从 AI 响应中提取有效 JSON)

### 8.4 天气服务

通过 `wttr.in` 免费 API 获取天气预报
- 端点: `https://wttr.in/{city}?format=j1&lang=zh`
- 返回: 天气描述、温度范围、风力、湿度、出行建议
- 用途: 旅行规划中嵌入天气信息到 AI 提示词

### 8.5 异步任务模式

AI 生成类接口采用异步模式:
1. 前端提交请求，后端立即返回任务 ID
2. 后端 `@Async` 异步执行 AI 调用
3. 前端通过轮询 (setInterval) 查询任务状态
4. 菜品菜谱: 每 2 秒轮询，最多 60 秒
5. 旅行规划: 每 3 秒轮询，直到 status 从 0 变为 1 或 2

---

## 9. 部署说明

### 9.1 本地开发

**后端:**
```bash
cd backend
# 使用 local profile (H2 内嵌数据库, AI 默认开启)
mvn spring-boot:run -Dspring.profiles.active=local
# 访问 API 文档: http://localhost:8080/doc.html
```

**前端:**
- 使用微信开发者工具导入 `frontend/` 目录
- `app.js` 中确保 `devMode: true` 使用开发登录
- AppID: `wx85b4f35c6645b395`

### 9.2 Docker Compose 部署

```bash
cd deploy
cp .env.example .env
# 编辑 .env 填入实际配置
docker-compose up -d
```

服务组成:

| 服务 | 镜像 | 端口 | 说明 |
|------|------|------|------|
| mysql | mysql:8.0 | 3306 | 自动从 sql/init.sql 初始化 |
| redis | redis:7-alpine | 6379 | 缓存服务 |
| backend | 构建自 Dockerfile | 8080 | Spring Boot 应用 |

数据卷: `mysql_data`, `redis_data`, `uploads_data`
网络: `ai-love-network` (bridge)

### 9.3 Linux 生产部署

```bash
cd deploy/linux-prod
cp .env.example .env
# 编辑 .env 配置数据库、Redis、微信等参数
vim .env
# 启动
./start.sh
# 停止
./stop.sh
```

`linux-prod/` 包含预构建的 `app.jar` (~52MB)，可直接部署。

### 9.4 MCP 自动部署 (Claude Code)

项目包含 MCP 部署服务器，可通过 Claude Code 一键部署:

配置文件: `.mcp.json` → 指向 `deploy/mcp/index.mjs`

**部署流程 (7 步):**
1. Maven 构建 (`mvn clean package -DskipTests`)
2. SSH 连接远程服务器
3. 停止旧进程 (远程执行 `stop.sh`)
4. SFTP 上传 JAR + start.sh + stop.sh
5. 写入 `.env` 配置文件
6. 启动服务 (远程执行 `start.sh`)
7. 健康检查 (轮询 `/api/auth/dev-login` 直到 HTTP 200)

**反向代理**: `deploy/mcp/setup-caddy.mjs` 配置 Caddy
- 域名: `yexwyu.xyz` → `127.0.0.1:8080`
- `www.yexwyu.xyz` 重定向到 `yexwyu.xyz`

### 9.5 环境变量说明

| 变量 | 说明 | 示例 |
|------|------|------|
| `SPRING_PROFILE` | Spring Profile | `prod` |
| `MYSQL_HOST` | MySQL 主机 | `127.0.0.1` |
| `MYSQL_PORT` | MySQL 端口 | `3306` |
| `MYSQL_DATABASE` | 数据库名 | `ai_love_daily` |
| `MYSQL_USERNAME` | MySQL 用户名 | `root` |
| `MYSQL_PASSWORD` | MySQL 密码 | — |
| `REDIS_HOST` | Redis 主机 | `127.0.0.1` |
| `REDIS_PORT` | Redis 端口 | `6379` |
| `REDIS_PASSWORD` | Redis 密码 | — |
| `REDIS_DATABASE` | Redis DB 编号 | `0` |
| `WX_APPID` | 微信小程序 AppID | — |
| `WX_SECRET` | 微信小程序 Secret | — |
| `JWT_SECRET` | JWT 签名密钥 | — |
| `FILE_UPLOAD_PATH` | 文件上传目录 | `/root/uploads` |
| `JAVA_OPTS` | JVM 参数 | `-Xms256m -Xmx512m` |
| `SERVER_PORT` | 服务端口 | `8080` |

### 9.6 定时任务

| 任务 | Cron 表达式 | 说明 |
|------|-------------|------|
| `AiDailyQuoteTask` | `0 0 6 * * ?` | 每天 6:00 为所有已绑定情侣预生成 AI 情话 |
| `PeriodReminderTask` | `0 0 8 * * ?` | 每天 8:00 检查即将到来的经期并记录提醒 |

---

## 10. 安全注意事项

### 10.1 已知安全风险

1. **明文凭据**: `application.yml`、`application-local.yml`、`deploy/mcp/.env` 中包含明文数据库密码、Redis 密码、AI API Key 和 SSH 密码
2. **CORS 全开**: `WebMvcConfig` 允许所有来源 (`allowedOrigins("*")`)
3. **无数据库外键**: 表间关系仅通过应用层维护
4. **Dev 登录暴露**: `/api/auth/dev-login` 在所有环境可用，生产环境应禁用

### 10.2 建议改进

- 将敏感凭据移至环境变量或密钥管理服务，添加 `.gitignore` 排除敏感文件
- 生产环境限制 CORS 来源为小程序域名
- 为 `dev-login` 接口添加 `@Profile("local")` 限制
- 考虑添加数据库外键约束增强数据完整性
- 文件上传添加类型白名单和大小限制
- 添加请求频率限制 (Rate Limiting)
- 定期轮换 API Key 和密码

---

*本文档基于项目源码自动生成，涵盖后端 12 个 Controller、13 个 Entity、54 个 API 接口，前端 10 个页面，12 张数据库表的完整分析。*
