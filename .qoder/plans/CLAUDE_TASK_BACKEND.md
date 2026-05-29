# Claude 后端开发任务包

> 你是后端工程师 Claude。请按本任务包独立完成"智能衣柜 AI 功能"的全部后端开发。
> 所有跨层接口必须严格遵守 `.qoder/plans/wardrobe-contract.md` 契约。
> 写入边界：仅可修改 `backend/**`、`sql/**`、`deploy/.env.example`、`deploy/linux-prod/.env.example`，不得修改 `frontend/**` 与契约文件。

## 0. 上下文阅读
请先阅读以下文件理解项目风格：
- `backend/src/main/resources/application.yml`
- `backend/src/main/java/com/ailovedaily/entity/DailyQuote.java`
- `backend/src/main/java/com/ailovedaily/service/impl/AiQuoteServiceImpl.java`
- `backend/src/main/java/com/ailovedaily/service/impl/HomeServiceImpl.java`
- `backend/src/main/java/com/ailovedaily/controller/HomeController.java`
- `backend/src/main/java/com/ailovedaily/controller/FileController.java`
- `backend/src/main/java/com/ailovedaily/service/FileService.java` 与其实现
- `backend/src/main/resources/schema-h2.sql`
- `.qoder/plans/wardrobe-contract.md`（强契约）

## 1. 任务清单

### 1.1 配置扩展
1. 修改 `backend/src/main/resources/application.yml`，在现有 `ai:` 段下追加 `vision`/`image` 子配置（见契约 §5）
2. 同步更新 `deploy/.env.example` 与 `deploy/linux-prod/.env.example`，追加：
   - `AI_VISION_API_URL`、`AI_VISION_MODEL`
   - `AI_IMAGE_API_URL`、`AI_IMAGE_MODEL`、`AI_IMAGE_ASPECT`、`AI_IMAGE_DAILY_LIMIT`

### 1.2 SQL
新建 `sql/upgrade-wardrobe.sql`，包含 4 张表 DDL（见契约 §1）+ 分类初始化数据。
同步追加到 `sql/init.sql`（MySQL 版本）和 `backend/src/main/resources/schema-h2.sql`（H2 兼容版本，注意 H2 不支持的语法改写）。

### 1.3 Entity（com.ailovedaily.entity 包）
- `WardrobeItem.java`：使用 `@TableName("wardrobe_item")`，参照 [DailyQuote.java] 风格，`deleted` 字段加 `@TableLogic`
- `Outfit.java`：使用 `@TableName("outfit")`，`deleted` 字段加 `@TableLogic`
- `OutfitItem.java`：使用 `@TableName("outfit_item")`
- `WardrobeCategory.java`：使用 `@TableName("wardrobe_category")`

注意：`season/occasion/ai_tags/item_ids` 在 entity 中用 String 字段保存 JSON 字符串，VO 转换时再 split 成 List。不引入额外 TypeHandler。

### 1.4 Mapper（com.ailovedaily.mapper 包）
- `WardrobeItemMapper.java extends BaseMapper<WardrobeItem>`，添加方法：
  - `List<WardrobeItem> selectByUserAndCategory(@Param("userId") Long userId, @Param("category") String category, @Param("season") String season)`
  - `Long countByUser(@Param("userId") Long userId)`
- `OutfitMapper.java extends BaseMapper<Outfit>`，添加 `List<Outfit> selectByUserOrderByCreateTime(@Param("userId") Long userId, @Param("offset") int offset, @Param("limit") int limit)`
- `OutfitItemMapper.java extends BaseMapper<OutfitItem>`
- `WardrobeCategoryMapper.java extends BaseMapper<WardrobeCategory>`

对应 XML 放在 `backend/src/main/resources/mapper/`。

### 1.5 VO / DTO（com.ailovedaily.vo 与 com.ailovedaily.dto 包）
完全按契约 §4 字段清单实现：
- `WardrobeItemVO`、`OutfitVO`、`AiRecognizeResultVO`
- `WardrobeItemUpdateDTO`、`OutfitGenerateDTO`

### 1.6 Service 接口（com.ailovedaily.service 包）
新建：
- `AiVisionService.recognizeClothing(String imageUrl) -> AiRecognizeResultVO`
- `AiImageService.generateOutfitImage(String prompt, List<String> referenceImages) -> String`（返回本地保存后的 URL）
- `WardrobeService`：
  - `WardrobeItemVO upload(MultipartFile file, Long userId)`
  - `List<WardrobeItemVO> list(Long userId, String category, String season, Boolean partnerView)`
  - `WardrobeItemVO detail(Long id, Long userId)`
  - `WardrobeItemVO update(Long id, Long userId, WardrobeItemUpdateDTO dto)`
  - `Boolean delete(Long id, Long userId)`
  - `Boolean favorite(Long id, Long userId)`
  - `WardrobeItemVO recognize(Long id, Long userId)`
- `OutfitService`：
  - `OutfitVO autoMatch(Long userId, OutfitGenerateDTO dto)`
  - `OutfitVO manualGenerate(Long userId, OutfitGenerateDTO dto)`
  - `List<OutfitVO> list(Long userId, int pageNum, int pageSize)`
  - `OutfitVO detail(Long id, Long userId)`
  - `Boolean delete(Long id, Long userId)`

### 1.7 Service 实现（com.ailovedaily.service.impl 包）

#### AiVisionServiceImpl
- 注入 `ai.vision.api-url` 与 `ai.vision.model` 配置
- 复用 `ai.api-key` 与 `ai.timeout`
- 模仿 `AiQuoteServiceImpl.callAiApi` 的 Hutool HTTP + JSONUtil 解析风格
- 视觉模型请求体（OpenAI 兼容格式）：
  ```json
  {
    "model": "abab-vision-pro",
    "messages": [
      {"role":"system","content":"你是衣物识别专家。严格只输出 JSON：{category,subType,color,style,season[],occasion[],tags[]}"},
      {"role":"user","content":[
        {"type":"text","text":"识别这件衣物"},
        {"type":"image_url","image_url":{"url":"<imageUrl>"}}
      ]}
    ],
    "temperature": 0.3
  }
  ```
- 解析 `choices[0].message.content`，剥离 markdown 代码块后用 JSONUtil 解析
- 解析失败返回 `AiRecognizeResultVO{success=false, category="top"}`
- 失败重试 1 次

#### AiImageServiceImpl
- 注入 `ai.image.api-url`、`ai.image.model`、`ai.image.aspect-ratio` 配置
- 调用 MiniMax 图像生成接口，请求体：
  ```json
  {"model":"image-01","prompt":"<prompt>","aspect_ratio":"3:4","n":1,"response_format":"url"}
  ```
- 解析 `data.image_urls[0]`，下载到 `uploads/outfit/yyyy/MM/dd/{uuid}.jpg`
- 调用 `FileService` 类似的存储工具完成落盘，返回相对 URL（如 `/uploads/outfit/2026/05/27/abc.jpg`）
- 单次失败抛 `BizException`

#### WardrobeServiceImpl
1. `upload`：先调 `FileService.uploadImageWithThumbnail(file, "wardrobe")` 拿到原图与缩略图 URL，再调 `aiVisionService.recognizeClothing(imageUrl)`，组装 entity 入库。AI 关闭或识别失败时仍入库 `aiRecognized=false`，category 默认 `top`。
2. `list`：根据 `partnerView`：
   - `false`：用当前 userId
   - `true`：从 `User.coupleId` 找出对方 userId（通过 `userMapper.selectPartnerByCoupleId`）；若无伴侣返回空列表
   - 调 `selectByUserAndCategory`
3. 鉴权：`detail/update/delete/favorite/recognize` 必须校验 `wardrobeItem.userId == userId`，否则抛 `BizException("无权操作")`
4. `favorite`：toggle 0↔1
5. VO 转换时把 JSON 字符串字段 split 成 List，反过来 update 时再 join

#### OutfitServiceImpl
1. `autoMatch`：
   - 拉取该用户全部衣物（结构化为简化 JSON：`[{id,categoryCode,color,style,season,occasion}]`）
   - 调用现有 `ai.api-url` 文本模型，要求严格输出 `{itemIds:[], reason:"...", visualPrompt:"..."}` JSON
   - 用 `visualPrompt` 调 `AiImageService.generateOutfitImage(visualPrompt, [所选衣物imageUrl...])`
   - 落库 `outfit` 与 `outfit_item`，返回 `OutfitVO`
2. `manualGenerate`：用户传 `itemIds + prompt`，跳过文本搭配，直接组合衣物属性生成 `visualPrompt` 调生图
3. 限流：在生图前 `Redis INCR outfit:gen:{userId}:{yyyyMMdd}`，超过 `ai.image.daily-limit` 抛 `BizException(429,"今日生成次数已达上限")`
4. AI 关闭时直接抛 `BizException(503,"AI 功能暂未启用")`

### 1.8 Controller（com.ailovedaily.controller 包）
- `WardrobeController` 与 `OutfitController`，路径与方法严格按契约 §3，参考 `HomeController` 风格使用 `@RequestAttribute("userId")` 注入
- 用 `@Tag` `@Operation` 加 swagger 注解
- 对 `partnerView` 参数加 `@RequestParam(defaultValue="false")`

### 1.9 首页入口
在 `HomeServiceImpl` 中查找现有快捷入口枚举位置（如 `quickStats` Map 中），追加 `wardrobeCount`：
```java
quickStats.put("wardrobeCount", wardrobeItemMapper.countByUser(userId));
```
若现有 `HomeServiceImpl` 没有可排序宫格枚举（已确认是写死在 wxml 里的），则只追加上述计数即可，由前端负责显示宫格入口。

### 1.10 测试
新建 `backend/src/test/java/com/ailovedaily/service/`：
- `WardrobeServiceTest.testRecognizeAndSave`：mock `AiVisionService` 返回固定 VO，断言 wardrobe_item 入库正确
- `OutfitServiceTest.testAutoMatch`：mock `AiVisionService` 与 `AiImageService`，断言 outfit + outfit_item 入库正确

测试可使用 `@SpringBootTest` 与 `@MockBean`，参考现有 test 目录其他测试。

### 1.11 编译校验
最后执行 `mvn clean package -DskipTests -f backend/pom.xml`，确保 BUILD SUCCESS。

## 2. 输出报告
完成后请输出：
1. 新增/修改的文件列表
2. mvn 编译结果（BUILD SUCCESS / FAILURE）
3. 任何与契约不一致的偏差及原因
