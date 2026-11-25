# MioWatery 原生 Android 重制计划

> 状态：规划与源码盘点阶段（2026-11-25）
> 目标：将 MioWatery 完整迁移为真正的 Android 原生应用。应用页面不得依赖 WebView、HTML、CSS 或 JavaScript 运行。

## 1. 项目边界

- 原生开发分支：`native-android`。
- 现有网页版本继续保留在 `main`，不在本阶段修改。
- 老师的原始仓库和用户原 fork 均不作为原生开发目标，不向其推送改动。
- 原生版最终独立构建 APK；网页版本只作为行为、功能和数据格式的迁移参考。
- 所有迁移均以源码核验为准，功能完成必须有实现和测试记录，不能仅以页面存在作为完成标准。

## 2. 当前源码事实

本计划基于 `water1014/MioWatery-Original` 的 `main` 分支源码盘点。

### 2.1 当前工程形态

- 主应用主体集中在根目录 `index.html`，约 1.7 MB。
- README 将其描述为单 HTML 浏览器应用，当前没有前端构建步骤。
- 另有 `server/music` 与 `server/repo` 两个服务端目录。
- 应用数据主要由一个名为 `xiaoshouji-v62-store` 的 IndexedDB 数据库保存，object store 为 `state`，主状态键为 `xsj`。
- `localStorage` 中的 `xsj` 是容量允许时保留的小型兼容副本。
- 音乐文件、字体文件、YouTube 媒体另有 IndexedDB 数据库：`xsj-music`、`xsj-fonts`、`xsj-yt`。
- Service Worker 用于网页端通知相关能力；原生版必须改为 Android 通知与后台机制，不移植 Service Worker。

### 2.2 已确认的状态域

源码默认状态及初始化逻辑中已确认以下主要数据域（不是最终数据库表清单）：

- API 与服务配置：主模型、备用模型、请求头、模型预设、温度、上下文长度、流式开关、语音 API、生图 API、NAI 参数、Embedding API。
- 用户与角色：用户资料、当前用户、角色列表、角色资料。
- 对话与记忆：聊天记录、按用户隔离的聊天、角色级配置、传统记忆、记忆条目、向量记忆、自动摘要计数、API 会话 ID。
- 世界书：世界书条目、内置世界书版本与已见记录。
- 桌面与外观：壁纸、图标位置、组件、排序、主题、字体、字体比例、CSS 主题与预设。
- 社交与日常：动态/帖子、日记、贴纸、个人资料、帖子装饰。
- 阅读：书籍、阅读目标、每日统计、阅读笔记、阅读聊天。
- 音乐与媒体：音乐资料库、播放记录、歌单、媒体文件。
- 扩展与连接：MCP、仓库服务、QQ 音乐配置、YouTube 数据及媒体。
- 其他：钱包、桌面组件、自定义小组件、时间与生活模块等。

注意：最终字段类型、默认值、迁移版本和关系需继续逐段核验源码，禁止直接把当前 JavaScript 对象未经校验地照搬成 Room 表。

## 3. 原生技术基线

- 语言：Kotlin。
- UI：Jetpack Compose + Material 3，并建立 MioWatery 自己的设计令牌。
- 导航：Navigation Compose。
- 本地关系数据：Room。
- 小型偏好：DataStore。
- 敏感凭据：Android Keystore 保护的加密存储；不得把 API Key 明文写入 APK、日志或普通备份。
- 网络：OkHttp / Retrofit；流式聊天使用可取消的流式读取。
- 图片：Coil。
- 音频：AndroidX Media3。
- 后台可延迟任务：WorkManager。
- 用户可感知且需持续运行的任务：按 Android 前台服务规则实现，并提供通知。
- 文件：Storage Access Framework / Photo Picker。
- 测试：JUnit、AndroidX Test、Compose UI Test。

具体依赖版本在初始化 Gradle 工程时选择当时稳定且相互兼容的版本，并锁定版本，不在文档中提前猜版本号。

## 4. 建议模块边界

初期保持单 Android app 工程，按 Gradle module 或 package 分层；当依赖和构建稳定后再拆独立 Gradle modules。

- `app`：Application、启动流程、主导航、系统栏与权限。
- `core/designsystem`：颜色、排版、间距、圆角、组件、主题。
- `core/database`：Room 数据库、Entity、DAO、Migration。
- `core/datastore`：非敏感偏好设置。
- `core/security`：凭据加密、密钥访问和脱敏。
- `core/network`：HTTP、SSE、超时、重试、错误映射。
- `core/media`：图片、音频、视频与文件处理。
- `core/model`：跨模块领域模型。
- `feature/home`：桌面、组件、导航入口。
- `feature/chat`：角色、对话、消息、流式响应。
- `feature/memory`：记忆、摘要、向量检索。
- `feature/worldbook`：世界书导入、编辑、触发与注入。
- `feature/settings`：模型、API、外观、语音、数据设置。
- `feature/image`：兼容图像 API、NovelAI、画廊与历史。
- 其余功能按源码盘点结果逐项建立 feature 包，不合并掉原有独立能力。

## 5. 迁移阶段与验收

### Phase 0：源码盘点与数据协议
- [x] 确认主仓库与主分支。
- [x] 创建独立原生开发分支。
- [x] 确认主状态 IndexedDB、兼容 localStorage 和额外媒体数据库。
- [ ] 完整列出全部入口、页面、弹层、操作与子功能。
- [ ] 完整列出状态字段、数据类型、关联关系和读写位置。
- [ ] 列出全部外部 API、请求头、请求体、响应处理和后端依赖。
- [ ] 定义导出包格式、导入校验、冲突处理和回滚策略。

### Phase 1：原生工程与系统骨架
- [x] Gradle / Kotlin / Compose 工程已完成首轮编译验证（Debug APK 构建成功）。
- [ ] 原生启动、导航、主题、设置与空状态。
- [ ] Room / DataStore / 安全存储基础设施。
- [x] GitHub Actions 已自动构建并上传 Debug APK 工件。

### Phase 2：聊天与 AI 核心
- [ ] 主/备用 OpenAI 兼容 API、模型拉取、自定义请求头。
- [ ] OpenCode Go 会话标识和请求行为。
- [ ] 角色、聊天、流式输出、取消、重试、消息操作。
- [ ] 世界书、提示词、上下文与记忆系统。
- [ ] 语音输入输出。

### Phase 3：全部应用功能
- [ ] 生图与 NovelAI 官方/第三方代理。
- [ ] 音乐、歌词、资料库、播放队列。
- [ ] 日记、动态、消息、资料、贴纸、相册。
- [ ] YouTube、仓库、阅读与其他应用。
- [ ] 钱包、MCP、Time is Nothing 及源码中确认的其余模块。
- [ ] 桌面组件、主题、字体和美化能力的原生替代。

### Phase 4：Android 系统适配
- [ ] 主动消息与后台调度。
- [ ] 通知权限、通知渠道、点击跳转。
- [ ] 音乐后台播放和媒体控制。
- [ ] 文件、图片、视频、系统分享。
- [ ] vivo / Android 16 状态栏、刘海屏、键盘与后台限制测试。

### Phase 5：数据迁移与发布
- [ ] 旧版导出包解析器。
- [ ] 数据映射、图片/音频文件迁移。
- [ ] 导入预览、冲突处理、事务回滚。
- [ ] 与旧版样本数据进行完整性核验。
- [ ] Release 签名与升级安装验证。
- [ ] GitHub Actions 发布 APK。

## 6. 数据迁移原则

1. 旧版数据只读提取，不在原生版首次启动时自动清理或覆盖旧数据。
2. 先识别导出包版本、内容类型、大小与校验信息，再开始导入。
3. 导入前展示可识别的角色、对话、世界书、记忆、日记、媒体数量。
4. 导入使用事务或可恢复批次；失败时保留原生数据库现状。
5. 大型媒体文件使用独立文件目录和数据库引用，不把所有二进制内容塞进单条 JSON。
6. API Key 等秘密默认不随普通备份导出；用户明确选择安全迁移时另行加密处理。
7. 迁移完成后抽样核对数量、时间戳、文本、附件和关系。

## 7. 外部服务清单（已从 README / 源码确认的初步项）

- OpenAI 兼容聊天接口：`/chat/completions`。
- 图像接口：NovelAI `/ai/generate-image`、OpenAI 兼容图像生成与参考图路径。
- Embeddings：`/embeddings`。
- 语音服务：配置的语音 API。
- MCP 解析服务：`/parse`、`/health`。
- 光遇连接：配置的服务 `/health` 与相关交互接口。
- QQ 音乐自建后端：`/api/music/*`，通过 `X-Access-Key`。
- 作品仓库：可配置服务器，包含上传、浏览、导入等 API。
- Open-Meteo 天气与地理编码。
- iTunes Search 音乐搜索兜底。
- YouTube 模块的媒体与内容来源需继续盘点，不能假定所有播放源均可直接原生播放。

所有服务都需要独立的超时、取消、错误提示、网络安全与凭据管理；第三方服务的 CORS 限制不应机械搬到原生客户端，但仍需遵守服务端鉴权和平台条款。

## 8. 当前风险与待核验点

- 单 HTML 主文件包含大量功能，页面 ID 不能完整代表实际功能数量。
- 部分数据按用户/角色分层保存，迁移时必须保留归属关系。
- 音乐、字体、YouTube 媒体存在额外 IndexedDB 存储，单纯迁移主状态会丢附件。
- 主动消息、定时器和网页通知依赖浏览器生命周期；Android 后台执行模型不同，必须重新设计。
- 自定义 CSS / HTML 小组件无法原样变成原生 UI。需要逐项定义原生替代能力，不能把 WebView 当作兜底。
- 作品仓库、QQ 音乐、MCP、光遇等功能依赖外部服务；服务可用性与 API 兼容性需要分别测试。
- 当前仓库许可证为 PolyForm Noncommercial 1.0.0。原生重制继续遵守该许可，不将项目包装为可商用产品。

## 9. 完成定义

一个功能只有在以下条件都满足时才标记完成：

- 原生 UI 和交互已实现，不通过 WebView 承载。
- 数据读写、空状态、加载态、错误态和边界情况已实现。
- 网络接口与旧版行为核验一致，或差异已明确记录。
- 单元 / UI 测试覆盖关键路径。
- 数据导入导出可验证。
- 在目标 Android 设备上完成实际测试。


## 10. 已落地的原生工程状态（2026-11-25）

- 已建立 `android/` 独立 Android 工程，包名 `com.water1014.miowatery`。
- 已完成 Kotlin + Jetpack Compose + Material 3 基础工程配置。
- 已实现原生启动入口、浅/深色主题、首页卡片、侧栏和占位页面切换。
- 已把源码中识别到的桌面应用入口纳入原生导航清单；目前均为迁移入口，不代表业务已实现。
- GitHub Actions 已成功构建 Debug APK。已验证的成功构建提交：`5f2aa8ef4bd41334dba3b69cdb6fdefaa14c1d38`。
- 后续首页导航扩展已提交，仍需等待该最新代码对应的 CI 构建验证。
- 当前没有接入旧版 IndexedDB，也没有写入、覆盖或清理旧版数据。
- 当前尚未进行 vivo S19 实机安装、性能、权限和后台运行测试。
