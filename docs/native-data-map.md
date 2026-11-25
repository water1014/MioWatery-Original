# MioWatery 原生数据映射（初步）

> 仅记录从当前源码可见的持久化边界和状态域。不是最终 Room Schema；字段类型、归属关系、附件引用和版本迁移仍需继续逐项核验。

## 1. 浏览器端持久化边界

| 存储 | 数据库 / 键 | 当前用途 | 原生迁移目标 |
|---|---|---|---|
| IndexedDB | `xiaoshouji-v62-store` / object store `state` / key `xsj` | 主应用状态 JSON | Room 数据库 + 文件附件目录 |
| localStorage | `xsj` | 主状态的小型兼容副本 | 不作为原生持久化方案 |
| IndexedDB | `xsj-music` / store `f` | 音乐文件 | App 私有媒体目录 + MediaAsset 表 |
| IndexedDB | `xsj-fonts` / store `f` | 自定义字体文件 | App 私有字体目录 + FontAsset 表 |
| IndexedDB | `xsj-yt` / store `f` | YouTube 媒体缓存 | App 私有媒体缓存目录 + MediaAsset 表 |

网页端主状态序列化时会从顶层对象中排除 `chats`，因为聊天集合按用户映射在 `chatsByUser` 中保存。迁移解析器必须尊重此结构，不可把遗漏的顶层 `chats` 误判为数据缺失。

## 2. 当前可见的顶层状态域

从源码中的 `S.<key>` 访问路径提取到以下集合。部分字段是临时状态、模块缓存或兼容字段，不能全部直接建表。

### 用户、角色与聊天
- `me`、`userProfiles`、`activeUserProfile`
- `personas`、`npcs`
- `chats`、`chatsByUser`
- `cc`、`memByUser`
- `apiSessions`、`last`

### AI 配置与记忆
- `cfg`、`apiPresets`、`secondaryPresets`
- `imagePresets`
- `globalPrompt`
- `cc` 中的角色级提示词、世界书关联和记忆设置
- `memByUser` 中按用户归属的记忆数据

### 世界书
- `worlds`、`wbCats`、`wbSeen`

### 桌面、主题与个性化
- `desktop`、`theme`、`themes`
- `fonts`、`fontPresets`、`fontActive`、`fontScale`
- `dcfg`、`pre`、`off`

### 社交、消息与日记
- `posts`、`postReplies`、`postDecorations`
- `feedSort`、`feedTrDefault`、`feedQuoteAvatar`、`feedQuoteText`
- `msgApp`、`msgFavs`
- `diaries`、`stickers`、`stickerGroups`
- `avatarLib`、`momentPosters`、`favCovers`

### 阅读、音乐与媒体
- `coread`、`music`、`linkCache`

### 生活、关系与钱包等模块
- `life`、`cp`、`iff`、`xinji`
- `wallet`、`uwallets`、`cwallets`、`walletAdopted`
- `rolePhones`、`rolePhoneLockText`、`rolePhoneLockWp`
- `mcpHist`

### 仓库与 YouTube
- `repo`
- `yt`、`ytBy`

## 3. 建议的原生数据边界

建议使用 Room 管理可查询、有关联和需要事务的数据；文件二进制不直接放入大型 Room 行。

| 原生领域 | 初步实体候选 | 说明 |
|---|---|---|
| 用户 | UserProfile | 当前用户和多用户切换 |
| 角色 | Persona、Npc | 角色设定、头像和配置 |
| 聊天 | Conversation、Message、MessageAttachment | 会话按用户和角色归属 |
| AI 设置 | ProviderConfig、ModelPreset、ApiSession | 凭据另走安全存储 |
| 记忆 | MemoryEntry、ConversationSummary、VectorMemory | 传统记忆与向量记忆需分别建模 |
| 世界书 | Worldbook、WorldbookCategory、WorldbookBinding | 触发词、范围、位置和角色关联 |
| 社交 | Post、PostReply、PostDecoration | 动态、评论和装饰 |
| 日记 | DiaryEntry、DiaryComment、DiaryAttachment | 日记正文和附件分离 |
| 媒体 | MediaAsset、MusicTrack、Playlist、PlaybackState | 文件本体放 App 私有目录 |
| 主题 | ThemePreset、FontAsset、DesktopLayout、WidgetConfig | 不执行任意网页 CSS |
| 外部连接 | ServiceEndpoint、ConnectionProfile | 凭据通过安全存储引用 |
| 生活模块 | LifeRecord、WalletEntry、ReadingRecord 等 | 细分后再确定实体 |

## 4. 迁移解析规则

- 解析旧版导出数据时保留原始 ID，另建旧 ID 到新 ID 的映射表。
- 对用户归属字段和角色归属字段进行显式映射，禁止把不同用户的聊天/记忆混合。
- 识别 Data URL、Base64、Blob 导出和 ZIP 附件，转换为文件并记录 SHA-256。
- 导入采用可重入设计：重复导入同一迁移包时提示已存在，不静默复制。
- 对未知字段保留原始 JSON 扩展数据，避免迁移初期丢失尚未识别的功能数据。
- 旧数据格式版本与原生数据库 schema 版本分别管理。
- API 密钥默认排除在普通迁移包之外；如支持密钥迁移，必须由用户明确选择并使用独立加密包。

## 5. 待完成核验

- [ ] 每个顶层字段的写入位置、读取位置、数据类型与默认值。
- [ ] 每个角色级 `cc` 字段的确切含义和迁移规则。
- [ ] `chatsByUser`、`memByUser` 的完整数据结构。
- [ ] 音乐、字体和 YouTube IndexedDB 中的 key 命名、Blob 类型和清理规则。
- [ ] 全量备份导出与导入的真实文件格式。
- [ ] 动态、日记、阅读、生活、钱包、角色手机等复杂子系统的内部对象结构。


## 6. 备份格式与聊天消息结构补充核验

### 6.1 全量备份

源码中的“导出 JSON 备份”会直接对当前状态对象执行 `JSON.stringify(S)`，文件名为 `xiaoshouji-backup-YYYY-MM-DD.json`。

“导出 ZIP 备份”会把同一份 JSON 放进 ZIP，内部文件名为 `xiaoshouji-backup.json`。当前源码实现的是 ZIP Stored（不压缩）条目。

旧版数据备份提示明确提醒：备份文件包含 API 密钥。原生版不能照搬这种默认行为；普通备份默认应剔除密钥，并提供独立的加密凭据迁移选项。

### 6.2 聊天消息基本字段

从聊天、线下、共读和多媒体消息写入代码中已看到，消息至少包含：

- 必需核心字段：`role`、`content`。
- 常见时间字段：`t`（毫秒时间戳）。
- 可选字段：`image`、`audio`、`quote`、`narr`、`stk`、`music`、`yt`、`life`、`xj`、`reasoning`、`recalled` 等。
- 有些消息的 `content` 为空但携带 `image`；不能把空文本消息视为无效记录。
- 聊天数组按联系人 ID 放在当前用户的 `chatsByUser[userId]` 映射中；切换用户时会切换对应聊天映射。
- 联系人级记忆字段包括 `traditionalMemory`、`memoryEntries`、`vectorMemories`、`lastAutoSummaryCount`，并从角色配置集合中迁入按用户隔离的 `memByUser`。

原生消息模型必须保留未知扩展字段，直到所有业务子系统完成映射；否则图片生成、音乐分享、YouTube 分享、表情包、引用、撤回、推理记录等内容会在迁移中丢失。

## 7. 下一轮数据结构核验

- [ ] 获取并分析真实导出 JSON 的完整顶层结构与各模块样例。
- [ ] 逐个追踪聊天消息可选字段的生成、显示、编辑、删除与导出路径。
- [ ] 核验多媒体附件是 Data URL、Blob 引用还是独立媒体数据库 ID。
- [ ] 核验聊天和角色级设置的完整归属规则。
- [ ] 基于字段核验结果再冻结 Room schema。
