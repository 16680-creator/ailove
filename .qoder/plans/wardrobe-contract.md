# 智能衣柜功能 跨层契约（不可变）

> 本文件由统筹方维护，是 Claude（后端）与 Codex（前端）共同遵守的接口契约。
> 任何字段、路径、JSON 结构若需变更必须先回到本文件统一修改。

## 1. 数据库 DDL（MySQL & H2 双兼容）

### 1.1 wardrobe_category（内置分类字典，初始化 5 行数据）
```sql
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
```
分类 code 取值范围：`top|bottom|coat|shoes|bag|inner|home`。

### 1.2 wardrobe_item
```sql
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
    season VARCHAR(255),       -- JSON 数组字符串，如 ["spring","summer"]
    occasion VARCHAR(255),     -- JSON 数组字符串，如 ["daily","date"]
    ai_tags VARCHAR(500),      -- JSON 数组字符串
    ai_recognized TINYINT DEFAULT 0,
    favorite TINYINT DEFAULT 0,
    wear_count INT DEFAULT 0,
    last_wear_at TIMESTAMP,
    deleted TINYINT DEFAULT 0,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_wardrobe_user_cat ON wardrobe_item(user_id, category_code, deleted);
```
枚举值（前后端统一）：
- season: `spring|summer|autumn|winter`
- occasion: `daily|work|date|sport|formal|home`

### 1.3 outfit
```sql
CREATE TABLE IF NOT EXISTS outfit (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    couple_id BIGINT,
    title VARCHAR(100),
    occasion VARCHAR(64),
    prompt VARCHAR(500),
    ai_generated_image_url VARCHAR(500),
    item_ids VARCHAR(500),     -- JSON 数组字符串，如 [12,15,21]
    reason VARCHAR(1000),
    deleted TINYINT DEFAULT 0,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_outfit_user ON outfit(user_id, create_time);
```

### 1.4 outfit_item
```sql
CREATE TABLE IF NOT EXISTS outfit_item (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    outfit_id BIGINT NOT NULL,
    wardrobe_item_id BIGINT NOT NULL,
    slot VARCHAR(32),
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_outfit_item_outfit ON outfit_item(outfit_id);
```

## 2. AI 视觉模型 JSON 契约

`AiVisionService.recognizeClothing(imageUrl)` 必须强制返回如下结构：
```json
{
  "category": "top",
  "subType": "T恤",
  "color": "白色",
  "style": "简约",
  "season": ["spring","summer"],
  "occasion": ["daily"],
  "tags": ["纯棉","圆领"]
}
```
- 解析失败时入库 `ai_recognized=0`，category 默认为 `top`

## 3. REST API 契约（统一前缀 `/api`）

所有接口都通过 `JwtInterceptor` 注入 `@RequestAttribute("userId")`。
返回体均为 `ResultVO<T>` 形如 `{code:200, message:"ok", data:T}`。

### 3.1 衣橱接口 `/api/wardrobe/**`
| Method | Path | 入参 | 出参 |
|---|---|---|---|
| POST | `/api/wardrobe/upload` | multipart `file` | `WardrobeItemVO`（含 AI 识别结果） |
| GET | `/api/wardrobe/list` | `category?`, `season?`, `partnerView=false\|true` | `List<WardrobeItemVO>` |
| GET | `/api/wardrobe/{id}` | path id | `WardrobeItemVO` |
| PUT | `/api/wardrobe/{id}` | body `WardrobeItemUpdateDTO` | `WardrobeItemVO` |
| DELETE | `/api/wardrobe/{id}` | path id | `Boolean` |
| POST | `/api/wardrobe/{id}/favorite` | path id | `Boolean`（toggle） |
| POST | `/api/wardrobe/{id}/recognize` | path id | `WardrobeItemVO`（重新识别） |

### 3.2 搭配接口 `/api/outfit/**`
| Method | Path | 入参 | 出参 |
|---|---|---|---|
| POST | `/api/outfit/auto-match` | `OutfitGenerateDTO {prompt, partnerView}` | `OutfitVO` |
| POST | `/api/outfit/manual` | `OutfitGenerateDTO {prompt, itemIds, partnerView}` | `OutfitVO` |
| GET | `/api/outfit/list` | `pageNum=1`, `pageSize=20` | `List<OutfitVO>` |
| GET | `/api/outfit/{id}` | path id | `OutfitVO` |
| DELETE | `/api/outfit/{id}` | path id | `Boolean` |

## 4. VO / DTO 字段清单

### WardrobeItemVO
```
Long id; Long userId; String imageUrl; String thumbUrl;
String categoryCode; String categoryName; String subType;
String color; String style;
List<String> season; List<String> occasion; List<String> tags;
Boolean aiRecognized; Boolean favorite;
Integer wearCount; LocalDateTime lastWearAt; LocalDateTime createTime;
```

### WardrobeItemUpdateDTO
```
String categoryCode; String subType; String color; String style;
List<String> season; List<String> occasion; List<String> tags;
```

### OutfitVO
```
Long id; Long userId; String title; String occasion;
String prompt; String aiGeneratedImageUrl; String reason;
List<WardrobeItemVO> items; LocalDateTime createTime;
```

### OutfitGenerateDTO
```
String prompt; List<Long> itemIds; Boolean partnerView;
```

### AiRecognizeResultVO
```
String category; String subType; String color; String style;
List<String> season; List<String> occasion; List<String> tags;
Boolean success;
```

## 5. application.yml 配置扩展

在现有 `ai:` 段下追加：
```yaml
ai:
  enabled: ${AI_ENABLED:false}
  api-url: ${AI_API_URL:https://api.minimax.chat/v1/text/chatcompletion_v2}
  api-key: ${AI_API_KEY:}
  model: ${AI_MODEL:MiniMax-M2.7}
  timeout: ${AI_TIMEOUT:300000}
  vision:
    api-url: ${AI_VISION_API_URL:https://api.minimax.chat/v1/text/chatcompletion_v2}
    model: ${AI_VISION_MODEL:abab-vision-pro}
  image:
    api-url: ${AI_IMAGE_API_URL:https://api.minimax.chat/v1/image_generation}
    model: ${AI_IMAGE_MODEL:image-01}
    aspect-ratio: ${AI_IMAGE_ASPECT:3:4}
    daily-limit: ${AI_IMAGE_DAILY_LIMIT:20}
```

## 6. 文件路径策略
- 上传衣物原图：`/uploads/wardrobe/yyyy/MM/dd/{uuid}.jpg`
- 缩略图：`/uploads/thumbnails/wardrobe/yyyy/MM/dd/{uuid}.jpg`
- AI 生成穿搭效果图：`/uploads/outfit/yyyy/MM/dd/{uuid}.jpg`

复用现有 `FileService.uploadImageWithThumbnail(file, "wardrobe")`。

## 7. 限流契约
- Redis Key：`outfit:gen:{userId}:{yyyyMMdd}`
- 每日上限：20，超限返回 `code=429, message="今日生成次数已达上限"`
- AI 关闭（`ai.enabled=false`）：
  - `/upload` 仍然落库但 `aiRecognized=false`，前端走"手动填属性"
  - `/auto-match` 与 `/manual` 直接 `code=503, message="AI 功能暂未启用"`

## 8. 前端 API 调用约定（utils/request.js 扩展）

```js
const wardrobeApi = {
  upload(filePath) { return uploadFile({ url: '/wardrobe/upload', filePath }); },
  list(params)     { return request({ url: '/wardrobe/list', data: params }); },
  detail(id)       { return request({ url: `/wardrobe/${id}` }); },
  update(id, data) { return request({ url: `/wardrobe/${id}`, method: 'PUT', data }); },
  remove(id)       { return request({ url: `/wardrobe/${id}`, method: 'DELETE' }); },
  favorite(id)     { return request({ url: `/wardrobe/${id}/favorite`, method: 'POST' }); },
  recognize(id)    { return request({ url: `/wardrobe/${id}/recognize`, method: 'POST' }); }
};
const outfitApi = {
  autoMatch(data)  { return request({ url: '/outfit/auto-match', method: 'POST', data }); },
  manual(data)     { return request({ url: '/outfit/manual', method: 'POST', data }); },
  list(params)     { return request({ url: '/outfit/list', data: params }); },
  detail(id)       { return request({ url: `/outfit/${id}` }); },
  remove(id)       { return request({ url: `/outfit/${id}`, method: 'DELETE' }); }
};
module.exports = { request, uploadFile, fixUrl, wardrobeApi, outfitApi };
```

## 9. 路径白名单（Agent 写入边界）
- Claude 仅可写：`backend/**`、`sql/**`、`deploy/.env.example`、`deploy/linux-prod/.env.example`
- Codex 仅可写：`frontend/**`
- 谁都不可改本契约文件 `.qoder/plans/wardrobe-contract.md`
