# 智能衣柜模块 — 自动化测试计划

> 适用范围：后端 12 个新增接口 + 前端 5 个新增 JS 文件 + 跨层契约一致性
> 执行入口：`.run-logs/test-wardrobe.ps1`
> 报告输出：`.run-logs/test-wardrobe.report.txt`

---

## 一、测试矩阵

| # | 用例名 | 类型 | 期望 | 阻塞前置 |
|---|--------|------|------|----------|
| T1 | mvn clean package -DskipTests | 编译 | BUILD SUCCESS | 无 |
| T2 | 启动 backend，等 dev-login 200 | 启动健康检查 | HTTP 200 + token | T1 |
| T3 | upgrade-wardrobe.sql 已被 schema-h2.sql 加载 | 元数据校验 | wardrobe_item 表存在 | T2 |
| T4 | GET /api/wardrobe/list?category=top | 空列表 | code=200, data=[] | T2 |
| T5 | GET /api/wardrobe/list?category=bottom&season=summer | 多过滤 | code=200, data=[] | T2 |
| T6 | GET /api/wardrobe/9999 | 404/异常路径 | code≠200 或 data=null | T2 |
| T7 | PUT /api/wardrobe/9999 | 异常路径 | code≠200 | T2 |
| T8 | POST /api/wardrobe/9999/favorite | 异常路径 | code≠200 | T2 |
| T9 | POST /api/wardrobe/9999/recognize | 异常路径 | code≠200 或 503 | T2 |
| T10 | GET /api/outfit/list?pageNum=1&pageSize=10 | 历史空列表 | code=200 | T2 |
| T11 | GET /api/outfit/9999 | 异常路径 | code≠200 | T2 |
| T12 | POST /api/outfit/auto-match {prompt:"..."} | AI 调用（可能 503） | code∈{200,429,503} | T2 |
| T13 | POST /api/outfit/manual {itemIds:[]} | 参数校验 | code≠200 | T2 |
| T14 | DELETE /api/outfit/9999 | 异常路径 | code≠200 | T2 |
| T15 | wardrobe.js / detail.js / outfit.js 语法 | 静态 | node --check 通过 | 无 |
| T16 | 后端 `@(Get/Post/Put/Delete)Mapping` 路径与前端 wardrobeApi/outfitApi 路径一致 | 契约一致性 | 全集匹配 | 无 |
| T17 | app.json 包含 3 个新页面 | 契约一致性 | 全集匹配 | 无 |
| T18 | utils/request.js 导出 wardrobeApi/outfitApi | 契约一致性 | 命中 | 无 |
| T19 | 首页 index.wxml 包含 wardrobe 入口 | 契约一致性 | 命中 | 无 |

**说明**：upload 接口和真实 AI 调用（vision/image）依赖外部 MiniMax 配额，自动测试不强制通过，预期降级到 503/429 也算 PASS。

---

## 二、脚本执行流程

```
[Phase 0 静态契约自检]   T15-T19  (无依赖，秒级)
          ↓
[Phase 1 编译]            T1       (~30s)
          ↓
[Phase 2 启动后端]        T2       (~25s)
          ↓
[Phase 3 API 冒烟]        T3-T14   (~10s)
          ↓
[Phase 4 收尾]            停 backend，输出 report
```

任意一个 Phase 失败立即终止后续 Phase（fail-fast），但同一 Phase 内的用例继续跑完。

---

## 三、安全约束

- 测试脚本只读不写任何业务数据库（全部用 9999 这种不存在的 id 走异常路径）
- 真实 upload / generate 不在自动测试覆盖范围（避免消耗 AI 额度），单独留人工冒烟用例
- 测试结束自动 stop backend，避免占用 8080 端口

---

## 四、人工冒烟（自动测试无法覆盖）

以下用例由人在微信开发者工具中执行：

1. 衣橱主页打开 → 看到 5 个分类 tab + 空状态
2. 上传一张衣物图片 → AI 识别 → 跳转详情页
3. 详情页 → 编辑分类/季节/标签 → 保存
4. 详情页 → 收藏切换
5. 衣橱主页 → 长按多选 2 件 → 去搭配 → 手动生成 → 看到效果图
6. 穿搭页 → 输入"今天约会"→ 自动搭配 → 看到效果图
7. 穿搭页 → 历史抽屉 → 看到历史记录
