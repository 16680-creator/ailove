# Codex 前端开发任务包

> 你是前端工程师 Codex。请按本任务包独立完成"智能衣柜 AI 功能"的全部前端开发。
> 所有跨层接口必须严格遵守 `.qoder/plans/wardrobe-contract.md` 契约。
> 写入边界：仅可修改 `frontend/**`，不得修改 `backend/**`、`sql/**` 与契约文件。

## 0. 上下文阅读
请先阅读以下文件理解项目风格：
- `frontend/app.json`
- `frontend/app.js`
- `frontend/utils/request.js`
- `frontend/pages/index/index.wxml`、`index.wxss`、`index.js`
- `frontend/pages/album/album.wxml`、`album.wxss`、`album.js`（参考宫格风格）
- `frontend/pages/diary/diary.wxml`（参考输入框 + 列表布局）
- `.qoder/plans/wardrobe-contract.md`（强契约）

## 1. 任务清单

### 1.1 注册页面
修改 `frontend/app.json`，在 `pages` 数组追加：
```
"pages/wardrobe/wardrobe",
"pages/wardrobe/detail",
"pages/outfit/outfit"
```
若需要新的 vant 组件（如 `van-search`、`van-popup`、`van-overlay`），在 `usingComponents` 中补充。

### 1.2 扩展 utils/request.js
按契约 §8 在 `frontend/utils/request.js` 末尾追加 `wardrobeApi` 与 `outfitApi`，并通过 `module.exports` 一并导出。
注意 `app.globalData.baseUrl` 已包含 `/api` 前缀，所以 url 直接写 `/wardrobe/...` 即可。
对应文件 `frontend/pages/wardrobe/wardrobe.js` 中只需 `const { wardrobeApi, outfitApi, fixUrl } = require('../../utils/request')`。

### 1.3 衣橱主页 `frontend/pages/wardrobe/wardrobe.{js,json,wxml,wxss}`
- 顶部 `van-tabs`：上衣/下装/外套/鞋子/包配饰（5 个 tab，对应 category code：top/bottom/coat/shoes/bag）
- 副筛选：季节标签 `<view class="season-chips">` + 切换"我的/TA 的"（segmented，状态字段 `partnerView: false|true`）
- 三列宫格：每个 cell 显示 `thumbUrl`（用 `fixUrl` 转完整地址）、`subType`、长按进入多选模式
- 多选模式底部出现"去搭配 (n)"按钮，跳到 `outfit` 页并通过全局 `wx.setStorageSync('selectedItemIds', [...])` 传递
- 右下角悬浮 + 按钮：`wx.chooseMedia` → `wardrobeApi.upload(filePath)` → 显示"AI 识别中…" loading（`wx.showLoading`）→ 上传成功后 `wx.navigateTo` 跳详情确认页
- 列表请求 `wardrobeApi.list({ category: 当前tab对应code, season: 选中的季节, partnerView })`，初次进入加 `van-skeleton` loading

### 1.4 衣物详情 `frontend/pages/wardrobe/detail.{js,json,wxml,wxss}`
- 顶部大图 + 返回按钮
- AI 属性卡片（每行一个 `van-cell`），可点击进入编辑模式，编辑后调 `wardrobeApi.update(id, dto)`
- 字段：分类（top/bottom/...），子类型，颜色，风格，季节多选 chips（spring/summer/autumn/winter），场合多选 chips（daily/work/date/sport/formal/home），自定义标签
- 操作栏：收藏（`wardrobeApi.favorite(id)`）/ 删除（confirm 后 `wardrobeApi.remove(id)`）/ 加入搭配（写入 storage 后跳 outfit 页）
- 当 `aiRecognized === false`，显示醒目提示 + "重新识别"按钮调 `wardrobeApi.recognize(id)`

### 1.5 搭配页 `frontend/pages/outfit/outfit.{js,json,wxml,wxss}`
- 页面顶部 `van-field type="textarea"` 输入框，placeholder "描述场合、温度、风格…"
- "AI 自动搭配"按钮（紫色渐变背景 + ⚡ icon），点击 `outfitApi.autoMatch({ prompt, partnerView: false })`
- 已选衣物缩略图横向滚动条（`<scroll-view scroll-x>`），从 storage `selectedItemIds` 读取并展示
- "手动生成搭配"按钮：`outfitApi.manual({ prompt, itemIds, partnerView })`
- 结果区：
  - 上方一张 AI 效果大图（`aiGeneratedImageUrl`，用 `fixUrl`）
  - 下方搭配理由 `reason`
  - 用到的衣物 chips（横排小卡片，点击跳详情）
  - "保存到我的搭配"按钮 → 后端已落库则提示"已保存"，无需二次调用
- 历史搭配区：可下拉切换"历史搭配"列表，调 `outfitApi.list({ pageNum:1, pageSize:20 })`
- AI 不可用错误提示：捕获后端 `code===503` 显示 toast，`code===429` 显示"今日额度用完"

### 1.6 首页入口接入
在 `frontend/pages/index/index.wxml` 的 `entry-grid` 内追加一项：
```html
<view class="entry-item" bindtap="navigateTo" data-url="/pages/wardrobe/wardrobe">
  <text class="entry-mark">衣</text>
  <text class="entry-name">智能衣橱</text>
  <text class="entry-meta">{{quickStats.wardrobeCount || 0}} 件衣物</text>
</view>
```
（注意：`navigateTo` 函数已存在于 `index.js`，无需新增）

如发现项目里有"可自定义排序宫格"的实现（如基于 `wx.getStorageSync('homeGridOrder')`），请将 `wardrobe` 也加入默认枚举数组的末尾。否则保持上述 wxml 写死方式即可。

### 1.7 图标占位
新增 `frontend/images/wardrobe.png`：可不下载真实图片，先创建一个 1x1 透明像素 PNG 作为占位（或留空交由后续替换），但务必保证 `app.json` 不要引用不存在的资源。本任务不强制要求图标文件存在。

### 1.8 样式约束
- 主色调沿用 `#bb775b` / `#fbf4eb` / `#fff9f2`
- 紫色渐变按钮颜色：`linear-gradient(135deg, #a18cd1 0%, #fbc2eb 100%)`
- 圆角统一 `12rpx`
- 列表卡片使用 `card` 类（参考 `index.wxss` 中已有定义）

### 1.9 编译校验
执行：
```powershell
node -e "require('./frontend/app.json')"
```
（仅校验 JSON 合法。微信小程序无 CLI 编译，请确保 wxml/wxss 语法正确即可）

## 2. 输出报告
完成后请输出：
1. 新增/修改的文件列表
2. 是否所有调用都通过 `wardrobeApi` / `outfitApi`
3. 任何与契约不一致的偏差及原因
