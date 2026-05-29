# 智能衣柜 AI 功能开发计划

## 任务一、需求拆解与边界
- 上传衣物图片 → MiniMax 视觉模型识别"类型/季节/风格/颜色/适合场合" → 入库分类
- 类淘宝分类展示：上衣/下装/外套/鞋子/包配饰，按 tab + 宫格 + 季节筛选
- 选中若干衣物 → AI 文案描述 + MiniMax 文生图生成穿搭效果图
- 用户自然语言指令（如"周五约会，温度 22 度"）→ AI 从衣橱中挑选组合 + 生成效果图
- 归属：个人衣橱（user_id），同时支持切换查看伴侣视角

## 任务二、AI 服务接入与配置扩展
**文件**：[application.yml](file:///d:/Code/aiCode/ai-love-daily/backend/src/main/resources/application.yml)
在现有 `ai:` 段下扩展两段子配置：
```yaml
ai:
  enabled: ${AI_ENABLED:false}
  api-url: ...    # 现有文本模型 (沿用)
  api-key: ...
  model: ...
  timeout: ...
  vision:
    api-url: ${AI_VISION_API_URL:https://api.minimax.chat/v1/text/chatcompletion_v2}
    model: ${AI_VISION_MODEL:abab-vision-pro}
  image:
    api-url: ${AI_IMAGE_API_URL:https://api.minimax.chat/v1/image_generation}
    model: ${AI_IMAGE_MODEL:image-01}
    aspect-ratio: ${AI_IMAGE_ASPECT:3:4}
```
同步更新 [deploy/.env.example](file:///d:/Code/aiCode/ai-love-daily/deploy/.env.example) 与 [deploy/linux-prod/.env.example](file:///d:/Code/aiCode/ai-love-daily/deploy/linux-prod/.env.example)。
> 备注：小米 MiMo tokenplan 仅支持图像理解（mimo-v2.5），不支持图像生成；为统一体验，本期视觉+生图都走 MiniMax。配置抽出后将来可零代码切回小米。

## 任务三、数据库设计（新建 4 张表）
新建升级 SQL：`/sql/upgrade-wardrobe.sql`，同时同步到 [schema-h2.sql](file:///d:/Code/aiCode/ai-love-daily/backend/src/main/resources/schema-h2.sql) 与 [init.sql](file:///d:/Code/aiCode/ai-love-daily/sql/init.sql)。

- `wardrobe_category`：内置分类字典（上衣/下装/外套/鞋子/包配饰/内搭/家居），不可改
- `wardrobe_item`：`id, user_id, image_url, thumb_url, category_code, sub_type, color, style, season(json), occasion(json), ai_tags(json), ai_recognized(0/1), favorite(0/1), wear_count, last_wear_at, created_at, deleted`
- `outfit`：`id, user_id, title, occasion, prompt, ai_generated_image_url, item_ids(json), reason, created_at, deleted`
- `outfit_item`：`outfit_id, wardrobe_item_id, slot`（用于穿搭与衣物的多对多关联，便于反查"被搭配过几次"）

索引：`wardrobe_item(user_id, category_code, deleted)`、`outfit(user_id, created_at)`。

## 任务四、后端实体/Mapper/VO（沿用 MyBatis-Plus 规范）
新增以下文件，分层与 [DailyQuote.java](file:///d:/Code/aiCode/ai-love-daily/backend/src/main/java/com/ailovedaily/entity/DailyQuote.java) 风格保持一致：
- `entity/WardrobeItem.java`、`entity/Outfit.java`、`entity/OutfitItem.java`
- `mapper/WardrobeItemMapper.java`（含 `selectByUserAndCategory`、`countByUser`）
- `mapper/OutfitMapper.java`、`mapper/OutfitItemMapper.java`
- `vo/WardrobeItemVO.java`、`vo/OutfitVO.java`、`vo/AiRecognizeResultVO.java`
- `dto/WardrobeUploadDTO.java`、`dto/OutfitGenerateDTO.java`（含 `prompt`、`itemIds`、`partnerView`）

## 任务五、后端 Service & AI 集成（核心）
模仿 [AiQuoteServiceImpl.java](file:///d:/Code/aiCode/ai-love-daily/backend/src/main/java/com/ailovedaily/service/impl/AiQuoteServiceImpl.java) 的容错与降级范式。

### 5.1 `AiVisionService`
- `recognizeClothing(String imageUrl)`：构造视觉 prompt（要求模型严格返回 JSON：`{category, subType, color, style, season[], occasion[], tags[]}`），调用 MiniMax `abab-vision-pro`，解析失败时回落为"未分类"
- 包含重试 1 次、超时 30s、失败抛 `BizException`

### 5.2 `AiImageService`
- `generateOutfitImage(String prompt, List<String> referenceImages)`：调用 MiniMax `image-01` 文生图，返回图片 URL
- 下载到本地 `/uploads/outfit/yyyy/MM/dd/`，复用现有 [FileController.java](file:///d:/Code/aiCode/ai-love-daily/backend/src/main/java/com/ailovedaily/controller/FileController.java) 的存储路径策略

### 5.3 `WardrobeService`
- `upload(file)`：复用现有上传工具落盘 → 调 `AiVisionService.recognizeClothing` → 入库 `wardrobe_item`
- `list(category, partnerView)`：根据 `partnerView` 字段决定查 `userId` 还是 `partnerUserId`（取自 `couple_link`）
- `delete / favorite / increaseWearCount` 等基础操作

### 5.4 `OutfitService`
- `autoMatch(prompt, partnerView)`：
  1. 拉取该用户全部衣物清单（结构化）
  2. 调用 mimo 文本模型，输入 prompt + 衣物 JSON，要求返回 `{itemIds:[], reason:"...", visualPrompt:"..."}`
  3. 用 `visualPrompt` 调 `AiImageService.generateOutfitImage`，参考图取所选衣物图
  4. 落库 `outfit` + `outfit_item`，返回 `OutfitVO`
- `manualGenerate(itemIds, prompt)`：用户手选 N 件 → 直接走步骤 3+4

## 任务六、Controller 与路由
新建 `WardrobeController`、`OutfitController`，参考 [HomeController.java](file:///d:/Code/aiCode/ai-love-daily/backend/src/main/java/com/ailovedaily/controller/HomeController.java) 风格：

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/wardrobe/upload` | multipart 上传 + AI 识别 |
| GET  | `/wardrobe/list` | 分类列表，参数 `category`、`partnerView` |
| GET  | `/wardrobe/{id}` | 详情 |
| PUT  | `/wardrobe/{id}` | 用户修正 AI 识别结果 |
| DELETE | `/wardrobe/{id}` | 软删除 |
| POST | `/outfit/auto-match` | 自然语言自动搭配 |
| POST | `/outfit/manual` | 手选生成效果图 |
| GET  | `/outfit/list` | 历史搭配 |

## 任务七、前端小程序页面
新增页面目录 `frontend/pages/wardrobe/`、`frontend/pages/wardrobe-detail/`、`frontend/pages/outfit/`，并在 [app.json](file:///d:/Code/aiCode/ai-love-daily/frontend/app.json) 注册。

### 7.1 衣橱主页 `wardrobe/index`
- 顶部 `van-tabs`：上衣 / 下装 / 外套 / 鞋子 / 包配饰
- 副筛选：季节（春夏秋冬）+ 我的/TA 的（情侣视角切换 segmented）
- 网格 3 列宫格，复用 [album](file:///d:/Code/aiCode/ai-love-daily/frontend/pages/album) 风格，长按多选进入"搭配模式"
- 右下角悬浮 `+` 按钮：拍照/相册 → 上传 → loading 框显示"AI 识别中…" → 跳详情确认页

### 7.2 衣物详情 `wardrobe-detail`
- 大图 + AI 识别属性卡片（可编辑修正）
- 操作：收藏 / 删除 / 加入搭配

### 7.3 搭配页 `outfit`
- 顶部输入框（vant `field`），placeholder："描述场合、温度、风格…"
- 下方"AI 自动搭配"按钮（紫色渐变 + ⚡图标）
- 已选衣物缩略图横向滚动条 + "手动搭配"按钮
- 结果区：上方一张 AI 效果大图，下方搭配理由 + 用到的衣物 chips
- "保存到我的搭配"按钮 → 落库 `outfit`

### 7.4 工具/网络
扩展 [frontend/utils/request.js](file:///d:/Code/aiCode/ai-love-daily/frontend/utils/request.js)（如已有）增加 `wardrobeApi`、`outfitApi`，并复用现有 token 拦截器。

## 任务八、首页入口接入
在 [home/HomeServiceImpl.java](file:///d:/Code/aiCode/ai-love-daily/backend/src/main/java/com/ailovedaily/service/impl/HomeServiceImpl.java) 与 [pages/index/index.js](file:///d:/Code/aiCode/ai-love-daily/frontend/pages/index/index.js) 的可排序宫格枚举中追加"智能衣橱"入口（参考记忆中的"首页入口宫格自定义排序"），icon 使用 `images/wardrobe.png`（需新增）。

## 任务九、容错、限流、成本控制
- AI 调用统一加 try/catch，识别失败时入库 `ai_recognized=0`，前端给"重新识别"按钮
- 单用户每日生成图片次数限 20 次（Redis key `outfit:gen:{userId}:{yyyyMMdd}` INCR）
- 上传图片 > 5MB 自动压缩（沿用现有 thumbnail 工具）
- AI 关闭（`ai.enabled=false`）时，识别走"手动填属性"分支，生成图功能直接禁用并提示

## 任务十、验证与发布
1. `mvn clean package -DskipTests` 编译通过
2. 用 H2 模式跑一次 e2e：上传 → 识别 → 自动搭配 → 生成图
3. 写最小冒烟测试：`WardrobeServiceTest.testRecognizeAndSave`、`OutfitServiceTest.testAutoMatch`（mock AI 客户端）
4. 编写并提交 `/sql/upgrade-wardrobe.sql` 到生产升级流程
5. 微信小程序真机测试：拍照上传 + 横屏效果图查看

## 交付物清单
- 4 张新表 + 升级 SQL
- 后端：3 个 Entity / 3 个 Mapper / 4 个 VO / 2 个 DTO / 4 个 Service / 2 个 Controller / 配置扩展
- 前端：3 个新页面 + 1 个首页入口接入
- 文档：在 [DESIGN.md](file:///d:/Code/aiCode/ai-love-daily/DESIGN.md) 追加"智能衣橱"章节
